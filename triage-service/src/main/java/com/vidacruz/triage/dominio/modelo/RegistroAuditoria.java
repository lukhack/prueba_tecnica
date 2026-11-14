package com.vidacruz.triage.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrada de auditoría clínica (RN-04): todo cambio de estado o de flag queda registrado
 * con timestamp, paciente, estado anterior, estado nuevo y tipo de evento.
 * Es inmutable y no existe operación de borrado en ningún puerto.
 */
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
        // El escalamiento es un cambio de flag: el estado clínico no cambia (sigue REGISTRADO).
        return new RegistroAuditoria(UUID.randomUUID(), paciente.id(), cuando,
                paciente.estado(), paciente.estado(), TipoEvento.PACIENTE_ESCALADO);
    }
}
