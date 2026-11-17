package com.vidacruz.triage.infraestructura.entrada.rest.dto;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;
import jakarta.validation.constraints.NotNull;

public record SolicitudCambiarEstado(
        @NotNull(message = "es obligatorio (REGISTRADO, EN_CONSULTA o ALTA)") EstadoPaciente estado) {
}
