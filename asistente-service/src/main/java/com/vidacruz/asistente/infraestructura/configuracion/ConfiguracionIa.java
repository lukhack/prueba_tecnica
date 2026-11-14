package com.vidacruz.asistente.infraestructura.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.asistente.infraestructura.salida.ia.CargadorCorpus;
import com.vidacruz.asistente.infraestructura.salida.ia.GeneradorRespuestasSpringAi;
import com.vidacruz.asistente.infraestructura.salida.ia.IndiceVectorialProtocolos;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Mono;

/**
 * Cableado de la IA (ver ADR-002 y ADR-003):
 * <ul>
 *   <li>Vector store en memoria de Spring AI alimentado por el modelo de embeddings del proveedor.</li>
 *   <li>Un único circuit breaker {@code proveedorIa} protege tanto embeddings como generación.</li>
 *   <li>El corpus se indexa en segundo plano al arrancar; el probe de readiness lo refleja.</li>
 * </ul>
 */
@Configuration
public class ConfiguracionIa {

    public static final String CIRCUIT_BREAKER_IA = "proveedorIa";

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public CargadorCorpus cargadorCorpus() {
        return new CargadorCorpus();
    }

    @Bean
    public IndiceVectorialProtocolos indiceVectorialProtocolos(VectorStore vectorStore, CargadorCorpus cargador,
                                                              CircuitBreakerRegistry registro) {
        return new IndiceVectorialProtocolos(vectorStore, cargador, registro.circuitBreaker(CIRCUIT_BREAKER_IA));
    }

    @Bean
    public GeneradorRespuestasSpringAi generadorRespuestas(ChatModel chatModel, ObjectMapper objectMapper,
                                                           CircuitBreakerRegistry registro, PropiedadesRag rag,
                                                           MeterRegistry meterRegistry) {
        return new GeneradorRespuestasSpringAi(chatModel, objectMapper, registro.circuitBreaker(CIRCUIT_BREAKER_IA),
                rag.timeoutLlm(), meterRegistry);
    }

    /** Health indicator "corpus": forma parte del grupo readiness (ver application.yml). */
    @Bean
    public ReactiveHealthIndicator corpusHealthIndicator(IndiceVectorialProtocolos indice) {
        return () -> Mono.fromSupplier(() -> indice.estaIndexado()
                ? Health.up().withDetail("corpus", "indexado").build()
                : Health.down().withDetail("corpus", "indexación pendiente").build());
    }

    @Bean
    public IndexadorAlArrancar indexadorAlArrancar(IndiceVectorialProtocolos indice) {
        return new IndexadorAlArrancar(indice);
    }

    /** Dispara la indexación cuando la aplicación está lista, sin bloquear el arranque. */
    public static class IndexadorAlArrancar {
        private final IndiceVectorialProtocolos indice;

        IndexadorAlArrancar(IndiceVectorialProtocolos indice) {
            this.indice = indice;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void alArrancar() {
            indice.indexarCorpus().subscribe();
        }
    }
}
