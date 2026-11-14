package com.vidacruz.asistente.dominio.modelo;

import java.util.List;

/**
 * Lo que devuelve el modelo generativo <em>antes</em> de ser validado por la política anti-alucinación.
 * Puede contener citas inexistentes o declarar que el contexto no cubre la pregunta.
 */
public record RespuestaGenerada(String respuesta, List<Fuente> fuentes, boolean sinProtocolo) {

    public RespuestaGenerada {
        fuentes = fuentes == null ? List.of() : List.copyOf(fuentes);
    }
}
