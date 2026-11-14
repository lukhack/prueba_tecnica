package com.vidacruz.asistente.infraestructura.salida.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.asistente.dominio.modelo.Fuente;
import com.vidacruz.asistente.dominio.modelo.RespuestaGenerada;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GeneradorRespuestasSpringAiTest {
    private final GeneradorRespuestasSpringAi generador = new GeneradorRespuestasSpringAi(
            mock(ChatModel.class), new ObjectMapper(), CircuitBreaker.ofDefaults("test"),
            Duration.ofSeconds(5), new SimpleMeterRegistry());

    @Test
    void parseaJsonEnvueltoEnMarcasDeCodigo() {
        String salida = """
                Claro, aquí tienes:
                ```json
                {"respuesta": "Reconfirmar la presión.", "fuentes": [{"protocolo": "PROTO-001", "seccion": "Medidas iniciales"}], "sinProtocolo": false}
                ```
                """;

        RespuestaGenerada r = generador.parsear(salida);

        assertThat(r.sinProtocolo()).isFalse();
        assertThat(r.respuesta()).isEqualTo("Reconfirmar la presión.");
        assertThat(r.fuentes()).containsExactly(new Fuente("PROTO-001", "Medidas iniciales"));
    }

    @Test
    void unaSalidaNoEstructuradaSeTrataComoSinProtocolo() {
        RespuestaGenerada r = generador.parsear("Lo siento, no puedo ayudar con eso.");

        assertThat(r.sinProtocolo()).isTrue();
        assertThat(r.fuentes()).isEmpty();
    }

    @Test
    void elPromptIncluyeElContextoYLaPregunta() {
        String consulta = GeneradorRespuestasSpringAi.construirConsulta("¿Qué hacer?",
                java.util.List.of(new com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo(
                        "PROTO-002", "Sepsis", "Acciones iniciales", "Lactato y hemocultivos.", 0.9)));

        assertThat(consulta).contains("protocolo: PROTO-002").contains("seccion: Acciones iniciales")
                .contains("Lactato y hemocultivos.").endsWith("PREGUNTA DEL MÉDICO: ¿Qué hacer?");
        assertThat(new Prompt(consulta).getContents()).isNotBlank();
    }
}
