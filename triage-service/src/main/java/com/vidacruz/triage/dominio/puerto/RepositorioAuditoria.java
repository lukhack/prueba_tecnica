package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.RegistroAuditoria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RepositorioAuditoria {
    Mono<RegistroAuditoria> registrar(RegistroAuditoria registro);

    Flux<RegistroAuditoria> buscarPorPaciente(UUID pacienteId);
}
