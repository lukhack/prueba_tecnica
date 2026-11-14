package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;

import java.time.Duration;

/**
 * Puerto de salida: SLA por nivel (RN-02). Los valores por defecto son los de la regla
 * (5, 10, 30, 60, 120 minutos) pero se cargan desde configuración para poder probar
 * el escalamiento sin esperar decenas de minutos.
 */
public interface PoliticaSla {

    Duration slaPara(NivelTriaje nivel);

    default long slaMinutos(NivelTriaje nivel) {
        return slaPara(nivel).toMinutes();
    }
}
