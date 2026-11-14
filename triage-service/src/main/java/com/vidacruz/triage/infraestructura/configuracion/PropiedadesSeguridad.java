package com.vidacruz.triage.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Seguridad de la API ({@code seguridad.*}). La clave llega por variable de entorno; nunca se commitea.
 *
 * @param apiKey clave que los clientes deben enviar en la cabecera {@code X-API-Key}
 */
@ConfigurationProperties(prefix = "seguridad")
public record PropiedadesSeguridad(String apiKey) {

    public PropiedadesSeguridad {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "La API key no está configurada: defina la variable de entorno TRIAGE_API_KEY (propiedad seguridad.api-key)");
        }
    }
}
