package com.vidacruz.asistente.dominio.puerto;

import com.vidacruz.asistente.dominio.modelo.EstadoUrgencias;
import com.vidacruz.asistente.dominio.modelo.EventoTriajeRecibido;
import reactor.core.publisher.Mono;

public interface VistaEstadoUrgencias {
    Mono<Boolean> aplicar(EventoTriajeRecibido evento);

    Mono<EstadoUrgencias> consultar();
}
