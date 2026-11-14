package com.vidacruz.triage.infraestructura.salida.persistencia;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.modelo.ResumenOperacionHora;
import com.vidacruz.triage.dominio.puerto.RepositorioReportes;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;

@Repository
public class AdaptadorRepositorioReportes implements RepositorioReportes {
    static final String SQL_RESUMEN_POR_HORA = """
            WITH primera_consulta AS (
                SELECT paciente_id, "timestamp" AS fecha_consulta
                  FROM (
                        SELECT paciente_id,
                               "timestamp",
                               ROW_NUMBER() OVER (PARTITION BY paciente_id ORDER BY "timestamp") AS orden
                          FROM auditoria_clinica
                         WHERE tipo_evento = 'PACIENTE_EN_CONSULTA'
                       ) consultas
                 WHERE orden = 1
            ),
            esperas AS (
                SELECT p.id,
                       p.nivel,
                       date_trunc('hour', p.fecha_registro, 'UTC') AS hora,
                       (EXTRACT(EPOCH FROM (pc.fecha_consulta - p.fecha_registro)) / 60.0)::double precision AS espera_minutos
                  FROM pacientes p
                  LEFT JOIN primera_consulta pc ON pc.paciente_id = p.id
                 WHERE p.fecha_registro >= :desde
                   AND p.fecha_registro <  :hasta
            )
            SELECT hora,
                   COUNT(*)                                                    AS total_registrados,
                   COUNT(*) FILTER (WHERE nivel = 'CRITICO')                   AS criticos,
                   COUNT(*) FILTER (WHERE nivel = 'URGENTE')                   AS urgentes,
                   COUNT(*) FILTER (WHERE nivel = 'URGENTE_2')                 AS urgentes_2,
                   COUNT(*) FILTER (WHERE nivel = 'NO_URGENTE')                AS no_urgentes,
                   COUNT(*) FILTER (WHERE nivel = 'NO_URGENTE_2')              AS no_urgentes_2,
                   AVG(espera_minutos)                                         AS espera_promedio_minutos,
                   PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY espera_minutos) AS espera_p95_minutos
              FROM esperas
             GROUP BY hora
             ORDER BY hora
            """;

    private final DatabaseClient db;

    public AdaptadorRepositorioReportes(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Flux<ResumenOperacionHora> resumenOperacionPorHora(Instant desde, Instant hasta) {
        return db.sql(SQL_RESUMEN_POR_HORA)
                .bind("desde", PacienteEntidad.aFecha(desde))
                .bind("hasta", PacienteEntidad.aFecha(hasta))
                .map(AdaptadorRepositorioReportes::aResumen)
                .all();
    }

    private static ResumenOperacionHora aResumen(Readable fila) {
        Map<NivelTriaje, Long> conteos = new EnumMap<>(NivelTriaje.class);
        conteos.put(NivelTriaje.CRITICO, fila.get("criticos", Long.class));
        conteos.put(NivelTriaje.URGENTE, fila.get("urgentes", Long.class));
        conteos.put(NivelTriaje.URGENTE_2, fila.get("urgentes_2", Long.class));
        conteos.put(NivelTriaje.NO_URGENTE, fila.get("no_urgentes", Long.class));
        conteos.put(NivelTriaje.NO_URGENTE_2, fila.get("no_urgentes_2", Long.class));
        return new ResumenOperacionHora(
                fila.get("hora", OffsetDateTime.class).toInstant(),
                fila.get("total_registrados", Long.class),
                conteos,
                fila.get("espera_promedio_minutos", Double.class),
                fila.get("espera_p95_minutos", Double.class));
    }
}
