package com.vidacruz.triage.infraestructura.entrada.rest;

import com.vidacruz.triage.aplicacion.comando.ComandoRegistrarPaciente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code POST /triage/pacientes}. Aquí solo se exige presencia de campos;
 * los rangos clínicos los valida el dominio ({@code SignosVitales}, {@code Paciente}).
 */
public record SolicitudRegistrarPaciente(
        @NotBlank(message = "es obligatorio") String nombreCompleto,
        @NotNull(message = "es obligatorio") Integer edad,
        @NotNull(message = "es obligatorio") Integer frecuenciaCardiaca,
        @NotNull(message = "es obligatorio") Integer presionSistolica,
        @NotNull(message = "es obligatorio") Integer presionDiastolica,
        @NotNull(message = "es obligatorio") Integer spo2,
        @NotNull(message = "es obligatorio") BigDecimal temperatura,
        @NotNull(message = "es obligatorio") Integer dolor,
        @NotNull(message = "es obligatorio") Integer gcs,
        @NotBlank(message = "es obligatoria") String quejaPrincipal) {

    public ComandoRegistrarPaciente aComando() {
        return new ComandoRegistrarPaciente(nombreCompleto, edad, frecuenciaCardiaca, presionSistolica,
                presionDiastolica, spo2, temperatura, dolor, gcs, quejaPrincipal);
    }
}
