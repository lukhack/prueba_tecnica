package com.vidacruz.triage.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración del monitor de SLA ({@code triage.escalamiento.*}).
 *
 * @param habilitado permite apagar el proceso de fondo (útil en tests)
 * @param intervalo  cada cuánto se evalúan los pacientes en espera
 */
@ConfigurationProperties(prefix = "triage.escalamiento")
public record PropiedadesEscalamiento(boolean habilitado, Duration intervalo) {

    public PropiedadesEscalamiento {
        if (intervalo == null) {
            intervalo = Duration.ofSeconds(30);
        }
    }
}
