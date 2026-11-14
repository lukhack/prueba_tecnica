package com.vidacruz.triage.infraestructura.salida.persistencia;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;
import com.vidacruz.triage.dominio.modelo.Paciente;
import com.vidacruz.triage.dominio.puerto.RepositorioPacientes;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@Repository
public class AdaptadorRepositorioPacientes implements RepositorioPacientes {
    private final R2dbcEntityTemplate template;
    private final DatabaseClient db;

    public AdaptadorRepositorioPacientes(R2dbcEntityTemplate template) {
        this.template = template;
        this.db = template.getDatabaseClient();
    }

    @Override
    public Mono<Paciente> guardarNuevo(Paciente paciente) {
        PacienteEntidad entidad = PacienteEntidad.desde(paciente);
        entidad.setVersion(0L);
        return template.insert(entidad).map(PacienteEntidad::aDominio);
    }

    @Override
    public Mono<Paciente> actualizar(Paciente paciente) {
        DatabaseClient.GenericExecuteSpec sentencia = db.sql("""
                        UPDATE pacientes
                           SET estado = :estado,
                               escalado = :escalado,
                               fecha_ultimo_cambio_estado = :fechaUltimoCambioEstado,
                               fecha_entrada_consulta = :fechaEntradaConsulta,
                               fecha_alta = :fechaAlta,
                               fecha_escalamiento = :fechaEscalamiento,
                               version = version + 1
                         WHERE id = :id
                        """)
                .bind("id", paciente.id())
                .bind("estado", paciente.estado().name())
                .bind("escalado", paciente.escalado())
                .bind("fechaUltimoCambioEstado", PacienteEntidad.aFecha(paciente.fechaUltimoCambioEstado()));
        sentencia = bindFechaOpcional(sentencia, "fechaEntradaConsulta", paciente.fechaEntradaConsulta());
        sentencia = bindFechaOpcional(sentencia, "fechaAlta", paciente.fechaAlta());
        sentencia = bindFechaOpcional(sentencia, "fechaEscalamiento", paciente.fechaEscalamiento());
        return sentencia.fetch().rowsUpdated().thenReturn(paciente);
    }

    private static DatabaseClient.GenericExecuteSpec bindFechaOpcional(DatabaseClient.GenericExecuteSpec spec,
                                                                        String nombre, Instant valor) {
        return valor == null
                ? spec.bindNull(nombre, OffsetDateTime.class)
                : spec.bind(nombre, PacienteEntidad.aFecha(valor));
    }

    @Override
    public Mono<Paciente> buscarPorId(UUID id) {
        return template.selectOne(query(where("id").is(id)), PacienteEntidad.class)
                .map(PacienteEntidad::aDominio);
    }

    @Override
    public Flux<Paciente> buscarRegistradosNoEscalados() {
        return template.select(
                        query(where("estado").is(EstadoPaciente.REGISTRADO.name()).and("escalado").isFalse())
                                .sort(org.springframework.data.domain.Sort.by("fechaRegistro")),
                        PacienteEntidad.class)
                .map(PacienteEntidad::aDominio);
    }

    @Override
    public Mono<Boolean> marcarEscaladoSiAplica(UUID id, Instant cuando) {
        return db.sql("""
                        UPDATE pacientes
                           SET escalado = TRUE,
                               fecha_escalamiento = :cuando,
                               version = version + 1
                         WHERE id = :id
                           AND estado = 'REGISTRADO'
                           AND escalado = FALSE
                        """)
                .bind("id", id)
                .bind("cuando", PacienteEntidad.aFecha(cuando))
                .fetch()
                .rowsUpdated()
                .map(filas -> filas > 0);
    }
}
