package com.vidacruz.asistente.dominio.modelo;

/**
 * Niveles de triaje tal como viajan en los eventos del servicio de Triage.
 * Es una copia deliberada: cada microservicio es dueño de su modelo (ver ADR-001).
 */
public enum NivelTriaje {
    CRITICO,
    URGENTE,
    URGENTE_2,
    NO_URGENTE,
    NO_URGENTE_2
}
