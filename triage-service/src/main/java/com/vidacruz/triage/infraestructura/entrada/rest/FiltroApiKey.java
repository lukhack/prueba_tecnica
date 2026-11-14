package com.vidacruz.triage.infraestructura.entrada.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.triage.infraestructura.configuracion.PropiedadesSeguridad;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Autenticación por API key en la cabecera {@code X-API-Key} (ver README, sección Seguridad).
 * Los endpoints de Actuator (probes de Kubernetes y métricas) quedan fuera del filtro.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroApiKey implements WebFilter {

    public static final String CABECERA = "X-API-Key";

    private final byte[] apiKeyEsperada;
    private final ObjectMapper objectMapper;

    public FiltroApiKey(PropiedadesSeguridad seguridad, ObjectMapper objectMapper) {
        this.apiKeyEsperada = seguridad.apiKey().getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String ruta = exchange.getRequest().getPath().value();
        if (ruta.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        String recibida = exchange.getRequest().getHeaders().getFirst(CABECERA);
        if (recibida != null && MessageDigest.isEqual(apiKeyEsperada, recibida.getBytes(StandardCharsets.UTF_8))) {
            return chain.filter(exchange);
        }
        return rechazar(exchange, ruta);
    }

    private Mono<Void> rechazar(ServerWebExchange exchange, String ruta) {
        ServerHttpResponse respuesta = exchange.getResponse();
        respuesta.setStatusCode(HttpStatus.UNAUTHORIZED);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        RespuestaError cuerpo = new RespuestaError(Instant.now(), 401, "No autorizado",
                "Cabecera " + CABECERA + " ausente o inválida", null, ruta);
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(cuerpo))
                .map(bytes -> respuesta.bufferFactory().wrap(bytes))
                .flatMap(buffer -> respuesta.writeWith(Mono.<DataBuffer>just(buffer)));
    }
}
