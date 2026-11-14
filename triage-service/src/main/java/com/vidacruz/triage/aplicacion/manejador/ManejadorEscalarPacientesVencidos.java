package com.vidacruz.triage.aplicacion.manejador;

import com.vidacruz.triage.dominio.modelo.EventoTriaje;
import com.vidacruz.triage.dominio.modelo.Paciente;
import com.vidacruz.triage.dominio.modelo.RegistroAuditoria;
import com.vidacruz.triage.dominio.modelo.TipoEvento;
import com.vidacruz.triage.dominio.puerto.MetricasTriaje;
import com.vidacruz.triage.dominio.puerto.NotificadorEquipoMedico;
import com.vidacruz.triage.dominio.puerto.PoliticaSla;
import com.vidacruz.triage.dominio.puerto.PublicadorEventos;
import com.vidacruz.triage.dominio.puerto.RepositorioAuditoria;
import com.vidacruz.triage.dominio.puerto.RepositorioPacientes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class ManejadorEscalarPacientesVencidos {
    private static final Logger log = LoggerFactory.getLogger(ManejadorEscalarPacientesVencidos.class);

    private final RepositorioPacientes pacientes;
    private final RepositorioAuditoria auditoria;
    private final PublicadorEventos publicador;
    private final NotificadorEquipoMedico notificador;
    private final PoliticaSla politicaSla;
    private final MetricasTriaje metricas;
    private final TransactionalOperator transacciones;
    private final Clock reloj;

    public ManejadorEscalarPacientesVencidos(RepositorioPacientes pacientes, RepositorioAuditoria auditoria,
                                             PublicadorEventos publicador, NotificadorEquipoMedico notificador,
                                             PoliticaSla politicaSla, MetricasTriaje metricas,
                                             TransactionalOperator transacciones, Clock reloj) {
        this.pacientes = pacientes;
        this.auditoria = auditoria;
        this.publicador = publicador;
        this.notificador = notificador;
        this.politicaSla = politicaSla;
        this.metricas = metricas;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public Mono<Long> ejecutar() {
        Instant ahora = reloj.instant();
        return pacientes.buscarRegistradosNoEscalados()
                .filter(p -> p.haSuperadoSla(politicaSla.slaPara(p.nivel()), ahora))

                .concatMap(p -> escalar(p, ahora))
                .count();
    }

    private Mono<Paciente> escalar(Paciente paciente, Instant ahora) {
        return pacientes.marcarEscaladoSiAplica(paciente.id(), ahora)
                .filter(Boolean::booleanValue)
                .flatMap(aplicado -> {
                    paciente.escalar(ahora);
                    return auditoria.registrar(RegistroAuditoria.deEscalamiento(paciente, ahora)).thenReturn(paciente);
                })
                .as(transacciones::transactional)
                .flatMap(p -> publicador.publicar(EventoTriaje.de(TipoEvento.PACIENTE_ESCALADO, p, ahora)).thenReturn(p))
                .doOnNext(p -> {
                    metricas.registrarEscalamiento(p.nivel());
                    log.warn("Paciente ESCALADO id={} nivel={} esperando={}min",
                            p.id(), p.nivel(), Duration.between(p.fechaRegistro(), ahora).toMinutes());
                })
                .flatMap(p -> notificador.notificarEscalamiento(p, Duration.between(p.fechaRegistro(), ahora))

                        .onErrorResume(error -> {
                            log.error("No se pudo notificar al equipo médico el escalamiento del paciente {}", p.id(), error);
                            return Mono.empty();
                        })
                        .thenReturn(p))
                .onErrorResume(error -> {
                    log.error("Error escalando paciente {}", paciente.id(), error);
                    return Mono.empty();
                });
    }
}
