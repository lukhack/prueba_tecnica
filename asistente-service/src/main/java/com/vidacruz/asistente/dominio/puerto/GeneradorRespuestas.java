package com.vidacruz.asistente.dominio.puerto;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.modelo.RespuestaGenerada;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GeneradorRespuestas {
    Mono<RespuestaGenerada> generar(String pregunta, List<FragmentoProtocolo> contexto);
}
