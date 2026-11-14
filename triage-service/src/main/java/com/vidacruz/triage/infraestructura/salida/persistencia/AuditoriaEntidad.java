package com.vidacruz.triage.infraestructura.salida.persistencia;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;
import com.vidacruz.triage.dominio.modelo.RegistroAuditoria;
import com.vidacruz.triage.dominio.modelo.TipoEvento;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Fila de la tabla {@code auditoria_clinica}. */
@Table("auditoria_clinica")
public class AuditoriaEntidad {

    @Id
    private UUID id;
    @Column("paciente_id")
    private UUID pacienteId;
    @Column("timestamp")
    private OffsetDateTime timestamp;
    @Column("estado_anterior")
    private String estadoAnterior;
    @Column("estado_nuevo")
    private String estadoNuevo;
    @Column("tipo_evento")
    private String tipoEvento;

    public static AuditoriaEntidad desde(RegistroAuditoria r) {
        AuditoriaEntidad e = new AuditoriaEntidad();
        e.id = r.id();
        e.pacienteId = r.pacienteId();
        e.timestamp = PacienteEntidad.aFecha(r.timestamp());
        e.estadoAnterior = r.estadoAnterior() == null ? null : r.estadoAnterior().name();
        e.estadoNuevo = r.estadoNuevo().name();
        e.tipoEvento = r.tipoEvento().name();
        return e;
    }

    public RegistroAuditoria aDominio() {
        return new RegistroAuditoria(id, pacienteId, PacienteEntidad.aInstante(timestamp),
                estadoAnterior == null ? null : EstadoPaciente.valueOf(estadoAnterior),
                EstadoPaciente.valueOf(estadoNuevo), TipoEvento.valueOf(tipoEvento));
    }

    public UUID getId() { return id; }
}
