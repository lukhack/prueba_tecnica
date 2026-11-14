package com.vidacruz.asistente.infraestructura.entrada.rest;

import jakarta.validation.constraints.NotBlank;

public record SolicitudConsulta(@NotBlank(message = "es obligatoria") String pregunta) {
}
