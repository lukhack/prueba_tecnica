package com.vidacruz.triage.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de integración publicado en el topic {@code triaje.eventos}.
 * Es el contrato público del servicio de Triage hacia otros servicios (ver docs/adr/ADR-003).
 */
public record EventoTriaje(
        UUID eventoId,
        String tipo,
        int version,
        UUID pacienteId,
        NivelTriaje nivel,
        EstadoPaciente estado,
        boolean escalado,
        Instant ocurridoEn) {

    public static final int VERSION_ACTUAL = 1;

    public static EventoTriaje de(TipoEvento tipoEvento, Paciente paciente, Instant ocurridoEn) {
        return new EventoTriaje(UUID.randomUUID(), tipoEvento.nombreEvento(), VERSION_ACTUAL,
                paciente.id(), paciente.nivel(), paciente.estado(), paciente.escalado(), ocurridoEn);
    }
}
