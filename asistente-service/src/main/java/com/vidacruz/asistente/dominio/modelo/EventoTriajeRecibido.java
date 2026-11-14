package com.vidacruz.asistente.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

public record EventoTriajeRecibido(
        UUID eventoId,
        String tipo,
        UUID pacienteId,
        NivelTriaje nivel,
        String estado,
        Instant ocurridoEn) {
    public static final String PACIENTE_REGISTRADO = "paciente_registrado";
    public static final String PACIENTE_EN_CONSULTA = "paciente_en_consulta";
    public static final String PACIENTE_ALTA = "paciente_alta";
    public static final String PACIENTE_ESCALADO = "paciente_escalado";

    public boolean pacienteActivo() {
        return !"ALTA".equals(estado);
    }

    public boolean esConocido() {
        return switch (tipo == null ? "" : tipo) {
            case PACIENTE_REGISTRADO, PACIENTE_EN_CONSULTA, PACIENTE_ALTA, PACIENTE_ESCALADO -> true;
            default -> false;
        };
    }
}
