package com.vidacruz.asistente.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seguridad")
public record PropiedadesSeguridad(String apiKey) {
    public PropiedadesSeguridad {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "La API key no está configurada: defina la variable de entorno ASISTENTE_API_KEY (propiedad seguridad.api-key)");
        }
    }
}
