package com.vidacruz.asistente.dominio.modelo;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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
