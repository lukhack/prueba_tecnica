package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.EventoTriaje;
import reactor.core.publisher.Mono;

public interface PublicadorEventos {
    Mono<Void> publicar(EventoTriaje evento);
}
