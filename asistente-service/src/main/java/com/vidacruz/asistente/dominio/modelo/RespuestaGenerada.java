package com.vidacruz.asistente.dominio.modelo;

import java.util.List;

public record RespuestaGenerada(String respuesta, List<Fuente> fuentes, boolean sinProtocolo) {
    public RespuestaGenerada {
        fuentes = fuentes == null ? List.of() : List.copyOf(fuentes);
    }
}
