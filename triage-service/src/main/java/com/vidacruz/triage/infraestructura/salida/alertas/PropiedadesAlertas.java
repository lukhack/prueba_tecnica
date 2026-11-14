package com.vidacruz.triage.infraestructura.salida.alertas;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración del canal de alertas al equipo médico ({@code triage.alertas.*}, ver ADR-004).
 *
 * @param webhookUrl URL del webhook (Slack/Teams/compatibles). Vacío → solo log estructurado.
 * @param timeout    tiempo máximo de espera por el webhook
 */
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
