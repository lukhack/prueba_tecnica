package com.vidacruz.triage.dominio.excepcion;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;

/** La transición de estado solicitada no está permitida por el flujo clínico (RF-02). HTTP 409. */
public class ExcepcionTransicionInvalida extends RuntimeException {

    public ExcepcionTransicionInvalida(EstadoPaciente actual, EstadoPaciente solicitado) {
        super("Transición inválida: un paciente en estado " + actual + " no puede pasar a " + solicitado
                + ". Transiciones permitidas: REGISTRADO→EN_CONSULTA, REGISTRADO→ALTA, EN_CONSULTA→ALTA");
    }
}
