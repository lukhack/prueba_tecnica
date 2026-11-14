package com.vidacruz.asistente.dominio.puerto;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface BuscadorProtocolos {
    Mono<List<FragmentoProtocolo>> buscar(String pregunta, int maximo);
}
