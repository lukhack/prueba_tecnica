package com.vidacruz.asistente.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "asistente.rag")
public record PropiedadesRag(int fragmentosARecuperar, double umbralSimilitud, Duration timeoutLlm) {
    public PropiedadesRag {
        if (fragmentosARecuperar <= 0) {
            fragmentosARecuperar = 4;
        }
        if (umbralSimilitud <= 0) {
            umbralSimilitud = 0.30;
        }
        if (timeoutLlm == null) {
            timeoutLlm = Duration.ofSeconds(20);
        }
    }
}
