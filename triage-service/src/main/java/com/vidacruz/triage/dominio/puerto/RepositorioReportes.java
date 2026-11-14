package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.ResumenOperacionHora;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * Puerto de salida: consultas analíticas (RF-05).
 * Los agregados se calculan en la base de datos, nunca iterando registros en memoria.
 */
public interface RepositorioReportes {

    Flux<ResumenOperacionHora> resumenOperacionPorHora(Instant desde, Instant hasta);
}
