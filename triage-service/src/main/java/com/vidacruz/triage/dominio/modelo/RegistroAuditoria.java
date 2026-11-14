package com.vidacruz.triage.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

public record RegistroAuditoria(
        UUID id,
        UUID pacienteId,
        Instant timestamp,
        EstadoPaciente estadoAnterior,
        EstadoPaciente estadoNuevo,
        TipoEvento tipoEvento) {
    public static RegistroAuditoria deRegistro(Paciente paciente) {
        return new RegistroAuditoria(UUID.randomUUID(), paciente.id(), paciente.fechaRegistro(),
                null, EstadoPaciente.REGISTRADO, TipoEvento.PACIENTE_REGISTRADO);
    }

    public static RegistroAuditoria deTransicion(Paciente paciente, EstadoPaciente anterior, Instant cuando) {
        return new RegistroAuditoria(UUID.randomUUID(), paciente.id(), cuando,
                anterior, paciente.estado(), TipoEvento.paraTransicionA(paciente.estado()));
    }

    public static RegistroAuditoria deEscalamiento(Paciente paciente, Instant cuando) {
        return new RegistroAuditoria(UUID.randomUUID(), paciente.id(), cuando,
                paciente.estado(), paciente.estado(), TipoEvento.PACIENTE_ESCALADO);
    }
}
