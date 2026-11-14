package com.vidacruz.triage.dominio.modelo;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados clínicos del paciente y sus transiciones válidas (RF-02).
 *
 * <pre>
 *   REGISTRADO ──► EN_CONSULTA ──► ALTA
 *        └──────────────────────────┘  (alta directa)
 * </pre>
 */
public enum EstadoPaciente {
    REGISTRADO,
    EN_CONSULTA,
    ALTA;

    private Set<EstadoPaciente> siguientes;

    static {
        REGISTRADO.siguientes = EnumSet.of(EN_CONSULTA, ALTA);
        EN_CONSULTA.siguientes = EnumSet.of(ALTA);
        ALTA.siguientes = EnumSet.noneOf(EstadoPaciente.class);
    }

    public boolean puedeTransicionarA(EstadoPaciente destino) {
        return siguientes.contains(destino);
    }

    /** Un paciente activo es todo aquel que no ha sido dado de alta. */
    public boolean esActivo() {
        return this != ALTA;
    }
}
