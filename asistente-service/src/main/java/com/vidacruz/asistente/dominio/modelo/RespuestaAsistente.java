package com.vidacruz.asistente.dominio.modelo;

import java.util.List;

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
