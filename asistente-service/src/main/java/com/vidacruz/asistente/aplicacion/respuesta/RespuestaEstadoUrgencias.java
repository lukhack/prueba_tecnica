package com.vidacruz.asistente.aplicacion.respuesta;

import com.vidacruz.asistente.dominio.modelo.EstadoUrgencias;
import com.vidacruz.asistente.dominio.modelo.NivelTriaje;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record RespuestaEstadoUrgencias(Map<String, Long> activosPorNivel, long totalActivos, Instant actualizadoEn) {
    public static RespuestaEstadoUrgencias de(EstadoUrgencias estado) {
        Map<String, Long> porNivel = new LinkedHashMap<>();
        for (NivelTriaje nivel : NivelTriaje.values()) {
            porNivel.put(nivel.name(), estado.activosPorNivel().get(nivel));
        }
        return new RespuestaEstadoUrgencias(porNivel, estado.totalActivos(), estado.actualizadoEn());
    }
}
