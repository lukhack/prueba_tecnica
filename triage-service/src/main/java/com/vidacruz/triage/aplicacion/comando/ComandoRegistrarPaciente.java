package com.vidacruz.triage.aplicacion.comando;

import java.math.BigDecimal;

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
