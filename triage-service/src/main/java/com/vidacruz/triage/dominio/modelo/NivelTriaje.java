package com.vidacruz.triage.dominio.modelo;

/**
 * Niveles de triaje definidos en la RN-01, ordenados de mayor a menor gravedad.
 * El orden del enum es significativo: un ordinal menor significa mayor gravedad.
 */
public enum NivelTriaje {
    CRITICO(1),
    URGENTE(2),
    URGENTE_2(3),
    NO_URGENTE(4),
    NO_URGENTE_2(5);

    private final int numero;

    NivelTriaje(int numero) {
        this.numero = numero;
    }

    public int numero() {
        return numero;
    }
}
