package com.vidacruz.asistente.infraestructura.salida.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.asistente.dominio.excepcion.ExcepcionProveedorIaNoDisponible;
import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.modelo.Fuente;
import com.vidacruz.asistente.dominio.modelo.RespuestaGenerada;
import com.vidacruz.asistente.dominio.puerto.GeneradorRespuestas;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GeneradorRespuestasSpringAi implements GeneradorRespuestas {
    static final String METRICA_LATENCIA_LLM = "asistente.llm.latencia";

    static final String INSTRUCCIONES = """
            Eres el asistente clínico interno de VidaCruz. Respondes a médicos de urgencias.
            REGLAS ESTRICTAS:
            1. Responde ÚNICAMENTE con información contenida en los fragmentos de protocolo que se te entregan como CONTEXTO.
            2. Nunca inventes, completes ni extrapoles contenido clínico. No uses conocimiento externo.
            3. Si el CONTEXTO no responde la pregunta, devuelve "sinProtocolo": true y deja "respuesta" vacía.
            4. Cita todas las fuentes usadas con el código de protocolo y el nombre EXACTO de la sección del contexto.
            5. Responde en español, de forma breve y precisa.
            FORMATO DE SALIDA: exclusivamente un objeto JSON válido, sin texto adicional ni marcas de código:
            {"respuesta": "...", "fuentes": [{"protocolo": "PROTO-XXX", "seccion": "..."}], "sinProtocolo": false}
            """;

    private static final Logger log = LoggerFactory.getLogger(GeneradorRespuestasSpringAi.class);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Duration timeout;
    private final Timer latencia;

    public GeneradorRespuestasSpringAi(ChatModel chatModel, ObjectMapper objectMapper, CircuitBreaker circuitBreaker,
                                       Duration timeout, MeterRegistry registry) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.timeout = timeout;
        this.latencia = Timer.builder(METRICA_LATENCIA_LLM)
                .description("Latencia de las llamadas al proveedor de IA")
                .publishPercentiles(0.5, 0.95)
                .register(registry);
    }

    @Override
    public Mono<RespuestaGenerada> generar(String pregunta, List<FragmentoProtocolo> contexto) {
        Prompt prompt = new Prompt(List.of(new SystemMessage(INSTRUCCIONES), new UserMessage(construirConsulta(pregunta, contexto))));
        long inicio = System.nanoTime();
        return chatModel.stream(prompt)
                .map(GeneradorRespuestasSpringAi::textoDe)
                .collect(StringBuilder::new, StringBuilder::append)
                .map(StringBuilder::toString)
                .timeout(timeout)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doFinally(s -> latencia.record(Duration.ofNanos(System.nanoTime() - inicio)))
                .onErrorMap(error -> new ExcepcionProveedorIaNoDisponible(error.getClass().getSimpleName() + ": " + error.getMessage(), error))
                .map(this::parsear);
    }

    static String construirConsulta(String pregunta, List<FragmentoProtocolo> contexto) {
        StringBuilder sb = new StringBuilder("CONTEXTO (fragmentos del corpus oficial):\n");
        for (FragmentoProtocolo f : contexto) {
            sb.append("--- protocolo: ").append(f.protocolo())
                    .append(" | titulo: ").append(f.titulo())
                    .append(" | seccion: ").append(f.seccion()).append('\n')
                    .append(f.contenido()).append('\n');
        }
        sb.append("---\nPREGUNTA DEL MÉDICO: ").append(pregunta);
        return sb.toString();
    }

    private static String textoDe(ChatResponse respuesta) {
        if (respuesta == null || respuesta.getResult() == null || respuesta.getResult().getOutput() == null) {
            return "";
        }
        String texto = respuesta.getResult().getOutput().getText();
        return texto == null ? "" : texto;
    }

    RespuestaGenerada parsear(String crudo) {
        String json = extraerJson(crudo);
        try {
            JsonNode nodo = objectMapper.readTree(json);
            boolean sinProtocolo = nodo.path("sinProtocolo").asBoolean(false);
            String respuesta = nodo.path("respuesta").asText("");
            List<Fuente> fuentes = new ArrayList<>();
            for (JsonNode f : nodo.path("fuentes")) {
                fuentes.add(new Fuente(f.path("protocolo").asText(""), f.path("seccion").asText("")));
            }
            return new RespuestaGenerada(respuesta, fuentes, sinProtocolo);
        } catch (Exception e) {
            log.warn("El modelo devolvió una salida no parseable; se descarta. Inicio: {}",
                    crudo.length() > 120 ? crudo.substring(0, 120) : crudo);
            return new RespuestaGenerada("", List.of(), true);
        }
    }

    private static String extraerJson(String crudo) {
        if (crudo == null) {
            return "";
        }
        int inicio = crudo.indexOf('{');
        int fin = crudo.lastIndexOf('}');
        return (inicio >= 0 && fin > inicio) ? crudo.substring(inicio, fin + 1) : crudo.trim();
    }
}
