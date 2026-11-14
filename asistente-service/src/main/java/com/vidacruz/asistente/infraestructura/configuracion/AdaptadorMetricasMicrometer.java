package com.vidacruz.asistente.infraestructura.configuracion;

import com.vidacruz.asistente.dominio.puerto.MetricasAsistente;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AdaptadorMetricasMicrometer implements MetricasAsistente {
    static final String METRICA_CONSULTAS = "asistente.consultas";

    private final Map<ResultadoConsulta, Counter> consultas = new EnumMap<>(ResultadoConsulta.class);

    public AdaptadorMetricasMicrometer(MeterRegistry registry) {
        for (ResultadoConsulta resultado : ResultadoConsulta.values()) {
            consultas.put(resultado, Counter.builder(METRICA_CONSULTAS)
                    .description("Consultas al asistente clínico por resultado")
                    .tag("resultado", resultado.name().toLowerCase())
                    .register(registry));
        }
    }

    @Override
    public void registrarConsulta(ResultadoConsulta resultado) {
        consultas.get(resultado).increment();
    }
}
