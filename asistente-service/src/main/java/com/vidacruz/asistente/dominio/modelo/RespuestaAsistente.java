package com.vidacruz.asistente.dominio.modelo;

import java.util.List;

/**
 * Respuesta final del asistente (RF-04 parte A). O bien está sustentada en al menos una fuente
 * válida del corpus, o bien es exclusivamente el aviso de "no existe protocolo disponible" (RN-05).
 */
public record RespuestaAsistente(String respuesta, List<Fuente> fuentes) {

    public static final String AVISO_SIN_PROTOCOLO =
            "No existe un protocolo disponible en el corpus oficial para esta consulta. "
                    + "No se genera contenido clínico sin respaldo documental.";

    public static RespuestaAsistente sinProtocoloDisponible() {
        return new RespuestaAsistente(AVISO_SIN_PROTOCOLO, List.of());
    }

    public static RespuestaAsistente sustentada(String respuesta, List<Fuente> fuentes) {
        if (fuentes == null || fuentes.isEmpty()) {
            throw new IllegalArgumentException("Una respuesta clínica debe citar al menos una fuente del corpus");
        }
        return new RespuestaAsistente(respuesta, List.copyOf(fuentes));
    }

    public boolean tieneFuentes() {
        return !fuentes.isEmpty();
    }
}
