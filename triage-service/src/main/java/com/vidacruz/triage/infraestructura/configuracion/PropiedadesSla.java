package com.vidacruz.triage.infraestructura.configuracion;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.puerto.PoliticaSla;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "triage.sla")
public record PropiedadesSla(
        Duration critico,
        Duration urgente,
        Duration urgente2,
        Duration noUrgente,
        Duration noUrgente2) implements PoliticaSla {
    public PropiedadesSla {
        critico = oPorDefecto(critico, 5);
        urgente = oPorDefecto(urgente, 10);
        urgente2 = oPorDefecto(urgente2, 30);
        noUrgente = oPorDefecto(noUrgente, 60);
        noUrgente2 = oPorDefecto(noUrgente2, 120);
    }

    private static Duration oPorDefecto(Duration valor, int minutos) {
        return valor == null ? Duration.ofMinutes(minutos) : valor;
    }

    @Override
    public Duration slaPara(NivelTriaje nivel) {
        return switch (nivel) {
            case CRITICO -> critico;
            case URGENTE -> urgente;
            case URGENTE_2 -> urgente2;
            case NO_URGENTE -> noUrgente;
            case NO_URGENTE_2 -> noUrgente2;
        };
    }
}
