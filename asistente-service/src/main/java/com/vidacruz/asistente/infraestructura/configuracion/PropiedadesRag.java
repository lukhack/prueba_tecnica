package com.vidacruz.asistente.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Parámetros del RAG ({@code asistente.rag.*}).
 *
 * @param fragmentosARecuperar cuántas secciones se recuperan del índice por pregunta (top-k)
 * @param umbralSimilitud      similitud mínima [0,1] para considerar un fragmento relevante
 * @param timeoutLlm           tiempo máximo de espera por el proveedor de IA
 */
@ConfigurationProperties(prefix = "asistente.rag")
public record PropiedadesRag(int fragmentosARecuperar, double umbralSimilitud, Duration timeoutLlm) {

    public PropiedadesRag {
        if (fragmentosARecuperar <= 0) {
            fragmentosARecuperar = 4;
        }
        if (umbralSimilitud <= 0) {
            umbralSimilitud = 0.30;
        }
        if (timeoutLlm == null) {
            timeoutLlm = Duration.ofSeconds(20);
        }
    }
}
