package com.vidacruz.asistente.infraestructura.entrada.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitudConsulta(@NotBlank(message = "es obligatoria") String pregunta) {
}
