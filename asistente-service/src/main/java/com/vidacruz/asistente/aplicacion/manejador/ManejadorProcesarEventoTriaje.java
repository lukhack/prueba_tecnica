package com.vidacruz.asistente.aplicacion.manejador;

import com.vidacruz.asistente.dominio.modelo.EventoTriajeRecibido;
import com.vidacruz.asistente.dominio.puerto.VistaEstadoUrgencias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Caso de uso: proyectar un evento de triaje sobre la vista de estado de urgencias. */
public class ManejadorProcesarEventoTriaje {

    private static final Logger log = LoggerFactory.getLogger(ManejadorProcesarEventoTriaje.class);

    private final VistaEstadoUrgencias vista;

    public ManejadorProcesarEventoTriaje(VistaEstadoUrgencias vista) {
        this.vista = vista;
    }

    public Mono<Void> ejecutar(EventoTriajeRecibido evento) {
        if (!evento.esConocido()) {
            log.warn("Evento de tipo desconocido ignorado: {}", evento.tipo());
            return Mono.empty();
        }
        if (evento.pacienteId() == null || evento.nivel() == null) {
            log.warn("Evento {} sin pacienteId o nivel, ignorado", evento.eventoId());
            return Mono.empty();
        }
        return vista.aplicar(evento)
                .doOnNext(cambio -> log.debug("Evento {} ({}) aplicado={} paciente={}",
                        evento.eventoId(), evento.tipo(), cambio, evento.pacienteId()))
                .then();
    }
}
