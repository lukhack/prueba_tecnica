package com.vidacruz.asistente.dominio.puerto;

import com.vidacruz.asistente.dominio.modelo.EstadoUrgencias;
import com.vidacruz.asistente.dominio.modelo.EventoTriajeRecibido;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida: vista materializada del estado de urgencias, reconstruida a partir de los
 * eventos de triaje (nunca llamando a la API del servicio de Triage).
 */
public interface VistaEstadoUrgencias {

    /**
     * Aplica un evento a la vista. Debe ser idempotente: Kafka entrega "al menos una vez" y el
     * mismo evento puede llegar repetido o fuera de orden.
     *
     * @return {@code true} si el evento modificó la vista
     */
    Mono<Boolean> aplicar(EventoTriajeRecibido evento);

    Mono<EstadoUrgencias> consultar();
}
