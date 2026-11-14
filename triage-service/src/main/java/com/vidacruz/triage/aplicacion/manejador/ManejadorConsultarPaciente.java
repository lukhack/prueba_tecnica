package com.vidacruz.triage.aplicacion.manejador;

import com.vidacruz.triage.aplicacion.respuesta.RespuestaPaciente;
import com.vidacruz.triage.dominio.excepcion.ExcepcionPacienteNoEncontrado;
import com.vidacruz.triage.dominio.puerto.PoliticaSla;
import com.vidacruz.triage.dominio.puerto.RepositorioPacientes;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.UUID;

/** Caso de uso RF-02 (consulta): datos completos, estado, flags y tiempo en el estado actual. */
public class ManejadorConsultarPaciente {

    private final RepositorioPacientes pacientes;
    private final PoliticaSla politicaSla;
    private final Clock reloj;

    public ManejadorConsultarPaciente(RepositorioPacientes pacientes, PoliticaSla politicaSla, Clock reloj) {
        this.pacientes = pacientes;
        this.politicaSla = politicaSla;
        this.reloj = reloj;
    }

    public Mono<RespuestaPaciente> ejecutar(UUID id) {
        return pacientes.buscarPorId(id)
                .switchIfEmpty(Mono.error(() -> new ExcepcionPacienteNoEncontrado(id)))
                .map(p -> RespuestaPaciente.de(p, politicaSla.slaMinutos(p.nivel()), reloj.instant()));
    }
}
