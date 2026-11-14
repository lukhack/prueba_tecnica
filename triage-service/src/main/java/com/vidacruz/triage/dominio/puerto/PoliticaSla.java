package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;

import java.time.Duration;

public interface PoliticaSla {
    Duration slaPara(NivelTriaje nivel);

    default long slaMinutos(NivelTriaje nivel) {
        return slaPara(nivel).toMinutes();
    }
}
