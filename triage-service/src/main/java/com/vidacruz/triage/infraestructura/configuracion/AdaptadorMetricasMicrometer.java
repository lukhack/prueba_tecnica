package com.vidacruz.triage.infraestructura.configuracion;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.puerto.MetricasTriaje;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** Adaptador Micrometer del puerto {@link MetricasTriaje}. Los contadores se registran al inicio para que existan en 0. */
@Component
public class AdaptadorMetricasMicrometer implements MetricasTriaje {

    static final String METRICA_CLASIFICACIONES = "triage.clasificaciones";
    static final String METRICA_ESCALAMIENTOS = "triage.escalamientos";

    private final Map<NivelTriaje, Counter> clasificaciones = new EnumMap<>(NivelTriaje.class);
    private final Map<NivelTriaje, Counter> escalamientos = new EnumMap<>(NivelTriaje.class);

    public AdaptadorMetricasMicrometer(MeterRegistry registry) {
        for (NivelTriaje nivel : NivelTriaje.values()) {
            clasificaciones.put(nivel, Counter.builder(METRICA_CLASIFICACIONES)
                    .description("Pacientes clasificados por nivel de triaje")
                    .tag("nivel", nivel.name())
                    .register(registry));
            escalamientos.put(nivel, Counter.builder(METRICA_ESCALAMIENTOS)
                    .description("Pacientes escalados por superar el SLA, por nivel")
                    .tag("nivel", nivel.name())
                    .register(registry));
        }
    }

    @Override
    public void registrarClasificacion(NivelTriaje nivel) {
        clasificaciones.get(nivel).increment();
    }

    @Override
    public void registrarEscalamiento(NivelTriaje nivel) {
        escalamientos.get(nivel).increment();
    }
}
