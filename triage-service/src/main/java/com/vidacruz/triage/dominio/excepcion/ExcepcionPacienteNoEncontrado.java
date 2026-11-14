package com.vidacruz.triage.dominio.excepcion;

import java.util.UUID;

public class ExcepcionPacienteNoEncontrado extends RuntimeException {
    public ExcepcionPacienteNoEncontrado(UUID id) {
        super("No existe un paciente con id " + id);
    }
}
