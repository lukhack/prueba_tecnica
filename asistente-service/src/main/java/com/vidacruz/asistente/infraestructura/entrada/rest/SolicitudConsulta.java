package com.vidacruz.asistente.infraestructura.entrada.rest;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de {@code POST /asistente/consulta}: {@code { "pregunta": "..." }}. */
public record SolicitudConsulta(@NotBlank(message = "es obligatoria") String pregunta) {
}
