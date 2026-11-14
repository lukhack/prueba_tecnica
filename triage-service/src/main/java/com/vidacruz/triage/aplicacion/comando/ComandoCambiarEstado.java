package com.vidacruz.triage.aplicacion.comando;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;

import java.util.UUID;

public record ComandoCambiarEstado(UUID pacienteId, EstadoPaciente nuevoEstado) {
}
