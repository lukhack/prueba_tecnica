package com.vidacruz.triage.infraestructura.entrada.rest;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code PATCH /triage/pacientes/{id}/estado}. */
public record SolicitudCambiarEstado(
        @NotNull(message = "es obligatorio (REGISTRADO, EN_CONSULTA o ALTA)") EstadoPaciente estado) {
}
