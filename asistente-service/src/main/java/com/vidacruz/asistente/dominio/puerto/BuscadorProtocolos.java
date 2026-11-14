package com.vidacruz.asistente.dominio.puerto;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import reactor.core.publisher.Mono;

import java.util.List;

/** Puerto de salida: recuperación (la "R" de RAG) de fragmentos del corpus relevantes para una pregunta. */
public interface BuscadorProtocolos {

    Mono<List<FragmentoProtocolo>> buscar(String pregunta, int maximo);
}
