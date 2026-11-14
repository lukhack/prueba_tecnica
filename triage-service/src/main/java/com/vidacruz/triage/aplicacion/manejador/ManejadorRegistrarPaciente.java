package com.vidacruz.triage.aplicacion.manejador;

import com.vidacruz.triage.aplicacion.comando.ComandoRegistrarPaciente;
import com.vidacruz.triage.aplicacion.respuesta.RespuestaPaciente;
import com.vidacruz.triage.dominio.modelo.EventoTriaje;
import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.modelo.Paciente;
import com.vidacruz.triage.dominio.modelo.RegistroAuditoria;
import com.vidacruz.triage.dominio.modelo.SignosVitales;
import com.vidacruz.triage.dominio.modelo.TipoEvento;
import com.vidacruz.triage.dominio.puerto.MetricasTriaje;
import com.vidacruz.triage.dominio.puerto.PoliticaSla;
import com.vidacruz.triage.dominio.puerto.PublicadorEventos;
import com.vidacruz.triage.dominio.puerto.RepositorioAuditoria;
import com.vidacruz.triage.dominio.puerto.RepositorioPacientes;
import com.vidacruz.triage.dominio.servicio.ClasificadorTriaje;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Clock;

/**
 * Caso de uso RF-01: registrar y clasificar un paciente.
 *
 * <ol>
 *   <li>Valida los datos y clasifica (dominio puro, síncrono y barato).</li>
 *   <li>Persiste paciente + auditoría en una misma transacción.</li>
 *   <li>Publica {@code paciente_registrado} una vez confirmada la transacción.</li>
 * </ol>
 */
public class ManejadorRegistrarPaciente {

    private static final Logger log = LoggerFactory.getLogger(ManejadorRegistrarPaciente.class);

    private final ClasificadorTriaje clasificador;
    private final RepositorioPacientes pacientes;
    private final RepositorioAuditoria auditoria;
    private final PublicadorEventos publicador;
    private final PoliticaSla politicaSla;
    private final MetricasTriaje metricas;
    private final TransactionalOperator transacciones;
    private final Clock reloj;

    public ManejadorRegistrarPaciente(ClasificadorTriaje clasificador, RepositorioPacientes pacientes,
                                      RepositorioAuditoria auditoria, PublicadorEventos publicador,
                                      PoliticaSla politicaSla, MetricasTriaje metricas,
                                      TransactionalOperator transacciones, Clock reloj) {
        this.clasificador = clasificador;
        this.pacientes = pacientes;
        this.auditoria = auditoria;
        this.publicador = publicador;
        this.politicaSla = politicaSla;
        this.metricas = metricas;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public Mono<RespuestaPaciente> ejecutar(ComandoRegistrarPaciente comando) {
        return Mono.fromSupplier(() -> crearPaciente(comando))
                .flatMap(this::persistirConAuditoria)
                .flatMap(paciente -> publicador
                        .publicar(EventoTriaje.de(TipoEvento.PACIENTE_REGISTRADO, paciente, paciente.fechaRegistro()))
                        .thenReturn(paciente))
                .doOnNext(paciente -> {
                    metricas.registrarClasificacion(paciente.nivel());
                    log.info("Paciente registrado id={} nivel={} sla={}min",
                            paciente.id(), paciente.nivel(), politicaSla.slaMinutos(paciente.nivel()));
                })
                .map(paciente -> RespuestaPaciente.de(paciente, politicaSla.slaMinutos(paciente.nivel()), reloj.instant()));
    }

    private Paciente crearPaciente(ComandoRegistrarPaciente c) {
        SignosVitales signos = new SignosVitales(c.frecuenciaCardiaca(), c.presionSistolica(), c.presionDiastolica(),
                c.spo2(), c.temperatura(), c.dolor(), c.gcs());
        NivelTriaje nivel = clasificador.clasificar(signos, c.quejaPrincipal());
        return Paciente.registrar(c.nombreCompleto(), c.edad(), signos, c.quejaPrincipal(), nivel, reloj.instant());
    }

    private Mono<Paciente> persistirConAuditoria(Paciente paciente) {
        return pacientes.guardarNuevo(paciente)
                .then(auditoria.registrar(RegistroAuditoria.deRegistro(paciente)))
                .thenReturn(paciente)
                .as(transacciones::transactional);
    }
}
