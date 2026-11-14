package com.vidacruz.asistente.dominio.modelo;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Vista materializada: pacientes activos (no dados de alta) por nivel de triaje (RF-04 parte B). */
public record EstadoUrgencias(Map<NivelTriaje, Long> activosPorNivel, Instant actualizadoEn) {

    public EstadoUrgencias {
        Map<NivelTriaje, Long> completo = new EnumMap<>(NivelTriaje.class);
        for (NivelTriaje nivel : NivelTriaje.values()) {
            completo.put(nivel, Math.max(0L, activosPorNivel.getOrDefault(nivel, 0L)));
        }
        activosPorNivel = Collections.unmodifiableMap(completo);
    }

    public long totalActivos() {
        return activosPorNivel.values().stream().mapToLong(Long::longValue).sum();
    }
}
