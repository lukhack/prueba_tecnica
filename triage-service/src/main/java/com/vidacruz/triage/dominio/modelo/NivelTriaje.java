package com.vidacruz.triage.dominio.modelo;

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
