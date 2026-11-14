package com.vidacruz.triage.infraestructura.entrada.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Formato único de error de la API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaError(
        Instant timestamp,
        int estado,
        String error,
        String mensaje,
        String campo,
        String ruta) {
}
