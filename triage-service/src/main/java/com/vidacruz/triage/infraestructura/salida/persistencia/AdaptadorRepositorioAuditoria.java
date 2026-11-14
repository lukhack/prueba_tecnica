package com.vidacruz.triage.infraestructura.salida.persistencia;

import com.vidacruz.triage.dominio.modelo.RegistroAuditoria;
import com.vidacruz.triage.dominio.puerto.RepositorioAuditoria;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

/**
 * Adaptador R2DBC del puerto {@link RepositorioAuditoria}.
 * Solo inserta y consulta: la inmutabilidad la refuerza además un trigger en la base de datos.
 */
@Repository
public class AdaptadorRepositorioAuditoria implements RepositorioAuditoria {

    private final R2dbcEntityTemplate template;

    public AdaptadorRepositorioAuditoria(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<RegistroAuditoria> registrar(RegistroAuditoria registro) {
        return template.insert(AuditoriaEntidad.desde(registro)).map(AuditoriaEntidad::aDominio);
    }

    @Override
    public Flux<RegistroAuditoria> buscarPorPaciente(UUID pacienteId) {
        return template.select(query(where("pacienteId").is(pacienteId)).sort(Sort.by("timestamp")), AuditoriaEntidad.class)
                .map(AuditoriaEntidad::aDominio);
    }
}
