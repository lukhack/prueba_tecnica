package com.vidacruz.triage.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

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
