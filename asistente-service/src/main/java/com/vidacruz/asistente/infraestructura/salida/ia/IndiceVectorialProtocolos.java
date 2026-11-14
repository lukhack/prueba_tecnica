package com.vidacruz.asistente.infraestructura.salida.ia;

import com.vidacruz.asistente.dominio.excepcion.ExcepcionProveedorIaNoDisponible;
import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.puerto.BuscadorProtocolos;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class IndiceVectorialProtocolos implements BuscadorProtocolos {
    static final String META_PROTOCOLO = "protocolo";
    static final String META_TITULO = "titulo";
    static final String META_SECCION = "seccion";

    private static final Logger log = LoggerFactory.getLogger(IndiceVectorialProtocolos.class);

    private final VectorStore vectorStore;
    private final CargadorCorpus cargador;
    private final CircuitBreaker circuitBreaker;
    private final AtomicBoolean indexado = new AtomicBoolean(false);

    public IndiceVectorialProtocolos(VectorStore vectorStore, CargadorCorpus cargador, CircuitBreaker circuitBreaker) {
        this.vectorStore = vectorStore;
        this.cargador = cargador;
        this.circuitBreaker = circuitBreaker;
    }

    public Mono<Void> indexarCorpus() {
        return Mono.fromRunnable(() -> {
                    List<Document> documentos = cargador.cargar().stream()
                            .map(IndiceVectorialProtocolos::aDocumento)
                            .toList();
                    vectorStore.add(documentos);
                    indexado.set(true);
                    log.info("Corpus indexado: {} fragmento(s) de protocolo", documentos.size());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5)).maxBackoff(Duration.ofMinutes(2))
                        .doBeforeRetry(señal -> log.warn("No se pudo indexar el corpus (intento {}): {}",
                                señal.totalRetries() + 1, señal.failure().getMessage())))
                .then();
    }

    public boolean estaIndexado() {
        return indexado.get();
    }

    @Override
    public Mono<List<FragmentoProtocolo>> buscar(String pregunta, int maximo) {
        if (!indexado.get()) {
            return Mono.error(new ExcepcionProveedorIaNoDisponible("el corpus aún no está indexado", null));
        }
        return Mono.fromCallable(() -> vectorStore.similaritySearch(
                        SearchRequest.builder().query(pregunta).topK(maximo).build()))
                .subscribeOn(Schedulers.boundedElastic())
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .map(documentos -> documentos.stream().map(IndiceVectorialProtocolos::aFragmento).toList())
                .onErrorMap(error -> !(error instanceof ExcepcionProveedorIaNoDisponible),
                        error -> new ExcepcionProveedorIaNoDisponible("embeddings: " + error.getMessage(), error));
    }

    static Document aDocumento(FragmentoProtocolo f) {
        String texto = f.protocolo() + " — " + f.titulo() + " — " + f.seccion() + ": " + f.contenido();
        return new Document(texto, Map.of(META_PROTOCOLO, f.protocolo(), META_TITULO, f.titulo(),
                META_SECCION, f.seccion(), "contenido", f.contenido()));
    }

    static FragmentoProtocolo aFragmento(Document d) {
        Map<String, Object> m = d.getMetadata();
        double puntaje = d.getScore() == null ? 0.0 : d.getScore();
        return new FragmentoProtocolo(
                String.valueOf(m.get(META_PROTOCOLO)),
                String.valueOf(m.get(META_TITULO)),
                String.valueOf(m.get(META_SECCION)),
                String.valueOf(m.getOrDefault("contenido", d.getText())),
                puntaje);
    }
}
