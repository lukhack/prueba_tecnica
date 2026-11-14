package com.vidacruz.triage.infraestructura.salida.mensajeria;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "triage.eventos")
public record PropiedadesEventos(String topic, int particiones) {
    public PropiedadesEventos {
        if (topic == null || topic.isBlank()) {
            topic = "triaje.eventos";
        }
        if (particiones <= 0) {
            particiones = 3;
        }
    }
}
