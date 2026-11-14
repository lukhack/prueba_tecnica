package com.vidacruz.asistente.aplicacion.manejador;

import com.vidacruz.asistente.aplicacion.respuesta.RespuestaEstadoUrgencias;
import com.vidacruz.asistente.dominio.puerto.VistaEstadoUrgencias;
import reactor.core.publisher.Mono;

/** Caso de uso RF-04 (parte B): estado de la urgencia en tiempo real. */
public class ManejadorConsultarEstadoUrgencias {

    private final VistaEstadoUrgencias vista;

    public ManejadorConsultarEstadoUrgencias(VistaEstadoUrgencias vista) {
        this.vista = vista;
    }

    public Mono<RespuestaEstadoUrgencias> ejecutar() {
        return vista.consultar().map(RespuestaEstadoUrgencias::de);
    }
}
