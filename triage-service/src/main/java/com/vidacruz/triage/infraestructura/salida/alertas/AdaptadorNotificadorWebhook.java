package com.vidacruz.triage.infraestructura.salida.alertas;

import com.vidacruz.triage.dominio.modelo.Paciente;
import com.vidacruz.triage.dominio.puerto.NotificadorEquipoMedico;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
public class AdaptadorNotificadorWebhook implements NotificadorEquipoMedico {
    static final String NOMBRE_CIRCUIT_BREAKER = "alertasWebhook";

    private static final Logger log = LoggerFactory.getLogger(AdaptadorNotificadorWebhook.class);
    private static final Logger logAlertas = LoggerFactory.getLogger("com.vidacruz.triage.ALERTAS");

    private final WebClient webClient;
    private final PropiedadesAlertas propiedades;
    private final CircuitBreaker circuitBreaker;

    public AdaptadorNotificadorWebhook(WebClient.Builder builder, PropiedadesAlertas propiedades,
                                       CircuitBreakerRegistry registro) {
        this.webClient = builder.build();
        this.propiedades = propiedades;
        this.circuitBreaker = registro.circuitBreaker(NOMBRE_CIRCUIT_BREAKER);
    }

    @Override
    public Mono<Void> notificarEscalamiento(Paciente paciente, Duration tiempoEsperando) {
        String texto = construirTexto(paciente, tiempoEsperando);

        logAlertas.warn("ALERTA_ESCALAMIENTO pacienteId={} nivel={} minutosEsperando={} mensaje=\"{}\"",
                paciente.id(), paciente.nivel(), tiempoEsperando.toMinutes(), texto);

        if (!propiedades.webhookConfigurado()) {
            return Mono.empty();
        }

        Map<String, Object> cuerpo = Map.of(
                "text", texto,
                "pacienteId", paciente.id().toString(),
                "nivel", paciente.nivel().name(),
                "minutosEsperando", tiempoEsperando.toMinutes(),
                "fechaRegistro", paciente.fechaRegistro().toString());

        return webClient.post()
                .uri(propiedades.webhookUrl())
                .bodyValue(cuerpo)
                .retrieve()
                .toBodilessEntity()
                .timeout(propiedades.timeout())
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                        .filter(error -> !(error instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException)))
                .doOnSuccess(r -> log.info("Alerta de escalamiento enviada al webhook para paciente {}", paciente.id()))
                .then();
    }

    static String construirTexto(Paciente p, Duration esperando) {
        return String.format("🚨 Paciente ESCALADO: %s (nivel %s) lleva %d min esperando consulta y superó su SLA. Id: %s",
                p.nombreCompleto(), p.nivel(), esperando.toMinutes(), p.id());
    }
}
