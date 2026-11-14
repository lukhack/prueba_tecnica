package com.vidacruz.triage.aplicacion.manejador;

import com.vidacruz.triage.aplicacion.comando.ComandoCambiarEstado;
import com.vidacruz.triage.aplicacion.respuesta.RespuestaPaciente;
import com.vidacruz.triage.dominio.excepcion.ExcepcionPacienteNoEncontrado;
import com.vidacruz.triage.dominio.modelo.EstadoPaciente;
import com.vidacruz.triage.dominio.modelo.EventoTriaje;
import com.vidacruz.triage.dominio.modelo.Paciente;
import com.vidacruz.triage.dominio.modelo.RegistroAuditoria;
import com.vidacruz.triage.dominio.modelo.TipoEvento;
import com.vidacruz.triage.dominio.puerto.PoliticaSla;
import com.vidacruz.triage.dominio.puerto.PublicadorEventos;
import com.vidacruz.triage.dominio.puerto.RepositorioAuditoria;
import com.vidacruz.triage.dominio.puerto.RepositorioPacientes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;

public class ManejadorCambiarEstado {
    private static final Logger log = LoggerFactory.getLogger(ManejadorCambiarEstado.class);

    private final RepositorioPacientes pacientes;
    private final RepositorioAuditoria auditoria;
    private final PublicadorEventos publicador;
    private final PoliticaSla politicaSla;
    private final TransactionalOperator transacciones;
    private final Clock reloj;

    public ManejadorCambiarEstado(RepositorioPacientes pacientes, RepositorioAuditoria auditoria,
                                  PublicadorEventos publicador, PoliticaSla politicaSla,
                                  TransactionalOperator transacciones, Clock reloj) {
        this.pacientes = pacientes;
        this.auditoria = auditoria;
        this.publicador = publicador;
        this.politicaSla = politicaSla;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public Mono<RespuestaPaciente> ejecutar(ComandoCambiarEstado comando) {
        return pacientes.buscarPorId(comando.pacienteId())
                .switchIfEmpty(Mono.error(() -> new ExcepcionPacienteNoEncontrado(comando.pacienteId())))
                .flatMap(paciente -> transicionar(paciente, comando.nuevoEstado()))
                .map(paciente -> RespuestaPaciente.de(paciente, politicaSla.slaMinutos(paciente.nivel()), reloj.instant()));
    }

    private Mono<Paciente> transicionar(Paciente paciente, EstadoPaciente nuevoEstado) {
        Instant ahora = reloj.instant();

        EstadoPaciente anterior = paciente.cambiarEstado(nuevoEstado, ahora);

        return pacientes.actualizar(paciente)
                .then(auditoria.registrar(RegistroAuditoria.deTransicion(paciente, anterior, ahora)))
                .thenReturn(paciente)
                .as(transacciones::transactional)
                .flatMap(p -> publicador
                        .publicar(EventoTriaje.de(TipoEvento.paraTransicionA(p.estado()), p, ahora))
                        .thenReturn(p))
                .doOnNext(p -> log.info("Paciente id={} pasó de {} a {}", p.id(), anterior, p.estado()));
    }
}
