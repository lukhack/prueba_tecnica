package com.vidacruz.triage.aplicacion.manejador;

import com.vidacruz.triage.aplicacion.respuesta.RespuestaReporteOperacion;
import com.vidacruz.triage.dominio.excepcion.ExcepcionValorInvalido;
import com.vidacruz.triage.dominio.puerto.RepositorioReportes;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/** Caso de uso RF-05: reporte de operación por hora en un rango de fechas. */
public class ManejadorReporteOperacion {

    /** Rango máximo consultable para evitar reportes desmedidos. */
    static final Duration RANGO_MAXIMO = Duration.ofDays(31);

    private final RepositorioReportes reportes;

    public ManejadorReporteOperacion(RepositorioReportes reportes) {
        this.reportes = reportes;
    }

    public Mono<RespuestaReporteOperacion> ejecutar(Instant desde, Instant hasta) {
        return Mono.fromRunnable(() -> validarRango(desde, hasta))
                .thenMany(reportes.resumenOperacionPorHora(desde, hasta))
                .map(RespuestaReporteOperacion.FilaHora::de)
                .collectList()
                .map(filas -> new RespuestaReporteOperacion(desde, hasta, filas));
    }

    private static void validarRango(Instant desde, Instant hasta) {
        if (desde == null || hasta == null) {
            throw new ExcepcionValorInvalido("desde", "Los parámetros 'desde' y 'hasta' son obligatorios (ISO 8601)");
        }
        if (!hasta.isAfter(desde)) {
            throw new ExcepcionValorInvalido("hasta", "'hasta' debe ser posterior a 'desde'");
        }
        if (Duration.between(desde, hasta).compareTo(RANGO_MAXIMO) > 0) {
            throw new ExcepcionValorInvalido("hasta", "El rango máximo del reporte es de " + RANGO_MAXIMO.toDays() + " días");
        }
    }
}
