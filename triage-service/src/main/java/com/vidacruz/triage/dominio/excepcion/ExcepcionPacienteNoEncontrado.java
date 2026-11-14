package com.vidacruz.triage.dominio.excepcion;

import java.util.UUID;

/** No existe un paciente con el identificador dado. HTTP 404. */
public class ExcepcionPacienteNoEncontrado extends RuntimeException {

    public ExcepcionPacienteNoEncontrado(UUID id) {
        super("No existe un paciente con id " + id);
    }
}
