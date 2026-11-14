package com.vidacruz.asistente.aplicacion.manejador;

import com.vidacruz.asistente.aplicacion.respuesta.RespuestaConsulta;
import com.vidacruz.asistente.dominio.excepcion.ExcepcionPreguntaInvalida;
import com.vidacruz.asistente.dominio.excepcion.ExcepcionProveedorIaNoDisponible;
import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.modelo.RespuestaAsistente;
import com.vidacruz.asistente.dominio.puerto.BuscadorProtocolos;
import com.vidacruz.asistente.dominio.puerto.GeneradorRespuestas;
import com.vidacruz.asistente.dominio.puerto.MetricasAsistente;
import com.vidacruz.asistente.dominio.puerto.MetricasAsistente.ResultadoConsulta;
import com.vidacruz.asistente.dominio.servicio.PoliticaAntiAlucinacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Caso de uso RF-04 (parte A): consulta en lenguaje natural con RAG.
 *
 * <pre>
 *   pregunta ─► recuperar fragmentos ─► ¿relevantes? ──no──► "no existe protocolo disponible"
 *                                          │ sí
 *                                          ▼
 *                                   generar con LLM ─► validar citas (RN-05) ─► respuesta
 * </pre>
 */
public class ManejadorConsultarAsistente {

    static final int LONGITUD_MAXIMA_PREGUNTA = 1_000;

    private static final Logger log = LoggerFactory.getLogger(ManejadorConsultarAsistente.class);

    private final BuscadorProtocolos buscador;
    private final GeneradorRespuestas generador;
    private final PoliticaAntiAlucinacion politica;
    private final MetricasAsistente metricas;
    private final int fragmentosARecuperar;

    public ManejadorConsultarAsistente(BuscadorProtocolos buscador, GeneradorRespuestas generador,
                                       PoliticaAntiAlucinacion politica, MetricasAsistente metricas,
                                       int fragmentosARecuperar) {
        this.buscador = buscador;
        this.generador = generador;
        this.politica = politica;
        this.metricas = metricas;
        this.fragmentosARecuperar = fragmentosARecuperar;
    }

    public Mono<RespuestaConsulta> ejecutar(String pregunta) {
        return Mono.fromSupplier(() -> validar(pregunta))
                .flatMap(p -> buscador.buscar(p, fragmentosARecuperar)
                        .map(politica::filtrarRelevantes)
                        .flatMap(relevantes -> responder(p, relevantes)))
                .doOnNext(r -> metricas.registrarConsulta(r.tieneFuentes()
                        ? ResultadoConsulta.CON_FUENTES : ResultadoConsulta.SIN_PROTOCOLO))
                .doOnError(ExcepcionProveedorIaNoDisponible.class,
                        e -> metricas.registrarConsulta(ResultadoConsulta.DEGRADADO))
                .map(RespuestaConsulta::de);
    }

    private Mono<RespuestaAsistente> responder(String pregunta, List<FragmentoProtocolo> relevantes) {
        if (relevantes.isEmpty()) {
            log.info("Sin fragmentos relevantes (umbral {}): no se consulta al modelo", politica.umbralSimilitud());
            return Mono.just(RespuestaAsistente.sinProtocoloDisponible());
        }
        return generador.generar(pregunta, relevantes)
                .map(generada -> politica.validar(generada, relevantes))
                .doOnNext(r -> log.info("Consulta respondida con {} fuente(s)", r.fuentes().size()));
    }

    private static String validar(String pregunta) {
        String limpia = pregunta == null ? "" : pregunta.trim();
        if (limpia.isEmpty()) {
            throw new ExcepcionPreguntaInvalida("La pregunta es obligatoria");
        }
        if (limpia.length() > LONGITUD_MAXIMA_PREGUNTA) {
            throw new ExcepcionPreguntaInvalida("La pregunta no puede superar " + LONGITUD_MAXIMA_PREGUNTA + " caracteres");
        }
        return limpia;
    }
}
