package com.vidacruz.asistente.dominio.puerto;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.modelo.RespuestaGenerada;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Puerto de salida: generación (la "G" de RAG) de una respuesta a partir de la pregunta y el contexto.
 * Si el proveedor de IA no está disponible, emite {@code ExcepcionProveedorIaNoDisponible}.
 */
public interface GeneradorRespuestas {

    Mono<RespuestaGenerada> generar(String pregunta, List<FragmentoProtocolo> contexto);
}
