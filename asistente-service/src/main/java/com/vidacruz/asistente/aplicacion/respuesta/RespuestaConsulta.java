package com.vidacruz.asistente.aplicacion.respuesta;

import com.vidacruz.asistente.dominio.modelo.Fuente;
import com.vidacruz.asistente.dominio.modelo.RespuestaAsistente;

import java.util.List;

public record RespuestaConsulta(String respuesta, List<Fuente> fuentes) {
    public static RespuestaConsulta de(RespuestaAsistente r) {
        return new RespuestaConsulta(r.respuesta(), r.fuentes());
    }
}
