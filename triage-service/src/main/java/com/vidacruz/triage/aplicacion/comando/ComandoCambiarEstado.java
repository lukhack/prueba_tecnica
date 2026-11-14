package com.vidacruz.triage.aplicacion.comando;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;

import java.util.UUID;

/** Intención de mover un paciente a un nuevo estado clínico (RF-02). */
public record ComandoCambiarEstado(UUID pacienteId, EstadoPaciente nuevoEstado) {
}
