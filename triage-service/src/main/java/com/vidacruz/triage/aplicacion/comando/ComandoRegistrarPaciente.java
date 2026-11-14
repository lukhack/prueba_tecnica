package com.vidacruz.triage.aplicacion.comando;

import java.math.BigDecimal;

/** Intención de registrar un paciente en recepción (RF-01). Datos crudos, aún sin validar. */
public record ComandoRegistrarPaciente(
        String nombreCompleto,
        int edad,
        int frecuenciaCardiaca,
        int presionSistolica,
        int presionDiastolica,
        int spo2,
        BigDecimal temperatura,
        int dolor,
        int gcs,
        String quejaPrincipal) {
}
