package com.vidacruz.triage.infraestructura.salida.alertas;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "triage.alertas")
public record PropiedadesAlertas(String webhookUrl, Duration timeout) {
    public PropiedadesAlertas {
        if (timeout == null) {
            timeout = Duration.ofSeconds(3);
        }
    }

    public boolean webhookConfigurado() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }
}
