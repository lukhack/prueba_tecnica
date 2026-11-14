package com.vidacruz.asistente.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento del topic {@code triaje.eventos} visto desde el asistente. Solo se leen los campos que
 * este servicio necesita ("lector tolerante"): el productor puede añadir campos sin romper nada.
 */
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

    /** Un paciente es activo mientras no esté en ALTA (RF-04 parte B). */
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
