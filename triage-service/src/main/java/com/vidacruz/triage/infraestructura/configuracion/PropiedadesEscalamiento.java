package com.vidacruz.triage.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "triage.escalamiento")
public record PropiedadesEscalamiento(boolean habilitado, Duration intervalo) {
    public PropiedadesEscalamiento {
        if (intervalo == null) {
            intervalo = Duration.ofSeconds(30);
        }
    }
}
