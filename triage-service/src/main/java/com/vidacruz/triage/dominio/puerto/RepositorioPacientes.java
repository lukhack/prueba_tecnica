package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.Paciente;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RepositorioPacientes {
    Mono<Paciente> guardarNuevo(Paciente paciente);

    Mono<Paciente> actualizar(Paciente paciente);

    Mono<Paciente> buscarPorId(UUID id);

    Flux<Paciente> buscarRegistradosNoEscalados();

    Mono<Boolean> marcarEscaladoSiAplica(UUID id, java.time.Instant cuando);
}
