package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;

/** Puerto de salida: métricas de negocio (conteo de clasificaciones por nivel y escalamientos). */
public interface MetricasTriaje {

    void registrarClasificacion(NivelTriaje nivel);

    void registrarEscalamiento(NivelTriaje nivel);
}
