package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;

public interface MetricasTriaje {
    void registrarClasificacion(NivelTriaje nivel);

    void registrarEscalamiento(NivelTriaje nivel);
}
