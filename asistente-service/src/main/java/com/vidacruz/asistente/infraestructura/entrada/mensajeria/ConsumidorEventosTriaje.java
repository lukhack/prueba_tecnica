package com.vidacruz.asistente.infraestructura.entrada.mensajeria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.asistente.aplicacion.manejador.ManejadorProcesarEventoTriaje;
import com.vidacruz.asistente.dominio.modelo.EventoTriajeRecibido;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Adaptador de entrada: consume {@code triaje.eventos} con reactor-kafka y proyecta cada evento sobre la vista.
 *
 * <p>Backpressure real: {@code concatMap} procesa un registro a la vez y solo se pide el siguiente cuando
 * el anterior terminó; el offset se confirma manualmente tras aplicar el evento (at-least-once).
 * La vista es idempotente, así que una re-entrega no altera los conteos.</p>
 */
@Component
public class ConsumidorEventosTriaje {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventosTriaje.class);

    private final KafkaReceiver<String, String> receiver;
    private final ManejadorProcesarEventoTriaje manejador;
    private final ObjectMapper objectMapper;
    private Disposable suscripcion;

    public ConsumidorEventosTriaje(KafkaReceiver<String, String> receiver, ManejadorProcesarEventoTriaje manejador,
                                   ObjectMapper objectMapper) {
        this.receiver = receiver;
        this.manejador = manejador;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        suscripcion = receiver.receive()
                .concatMap(this::procesar)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2)).maxBackoff(Duration.ofMinutes(1))
                        .doBeforeRetry(s -> log.error("Consumidor de triaje.eventos reiniciando tras error", s.failure())))
                .subscribe();
        log.info("Consumidor de triaje.eventos iniciado");
    }

    private Mono<Void> procesar(ReceiverRecord<String, String> registro) {
        return Mono.fromCallable(() -> objectMapper.readValue(registro.value(), EventoTriajeRecibido.class))
                .flatMap(manejador::ejecutar)
                .onErrorResume(error -> {
                    // Mensaje malformado o irrecuperable: se registra y se descarta para no bloquear la partición.
                    log.error("Evento descartado (offset {} partición {}): {}", registro.offset(), registro.partition(),
                            error.getMessage());
                    return Mono.empty();
                })
                .doFinally(s -> registro.receiverOffset().acknowledge());
    }

    @PreDestroy
    public void detener() {
        if (suscripcion != null) {
            suscripcion.dispose();
        }
    }
}
