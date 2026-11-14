package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.EventoTriaje;
import reactor.core.publisher.Mono;

/** Puerto de salida: publicación de eventos de integración (topic triaje.eventos). */
public interface PublicadorEventos {

    Mono<Void> publicar(EventoTriaje evento);
}
