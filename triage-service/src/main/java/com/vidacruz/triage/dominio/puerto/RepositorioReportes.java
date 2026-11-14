package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.ResumenOperacionHora;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface RepositorioReportes {
    Flux<ResumenOperacionHora> resumenOperacionPorHora(Instant desde, Instant hasta);
}
