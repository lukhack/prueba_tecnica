package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.Paciente;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Puerto de salida: persistencia del agregado Paciente. */
public interface RepositorioPacientes {

    Mono<Paciente> guardarNuevo(Paciente paciente);

    Mono<Paciente> actualizar(Paciente paciente);

    Mono<Paciente> buscarPorId(UUID id);

    /** Pacientes que siguen esperando consulta y aún no han sido escalados (candidatos a escalamiento). */
    Flux<Paciente> buscarRegistradosNoEscalados();

    /**
     * Marca el flag de escalado de forma atómica: solo tiene efecto si el paciente sigue
     * REGISTRADO y no estaba escalado. Garantiza la RN-03 incluso con varias instancias del servicio.
     *
     * @return {@code true} si esta llamada fue la que aplicó el escalamiento
     */
    Mono<Boolean> marcarEscaladoSiAplica(UUID id, java.time.Instant cuando);
}
