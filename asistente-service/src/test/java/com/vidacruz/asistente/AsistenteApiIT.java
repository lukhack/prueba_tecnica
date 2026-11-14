package com.vidacruz.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.asistente.ModelosIaFalsos.ChatModelFalso;
import com.vidacruz.asistente.ModelosIaFalsos.ChatModelFalso.Modo;
import com.vidacruz.asistente.dominio.modelo.RespuestaAsistente;
import com.vidacruz.asistente.infraestructura.entrada.rest.FiltroApiKey;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración del Servicio de Asistente con Kafka y Redis reales (Testcontainers) y proveedor
 * de IA simulado ({@link ModelosIaFalsos}): RAG con citas, anti-alucinación, modo degradado y
 * reconstrucción del estado de urgencias a partir de eventos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "seguridad.api-key=clave-de-prueba",
        "spring.ai.model.chat=none",                 // se reemplazan por los dobles de ModelosIaFalsos
        "spring.ai.model.embedding=none",
        "spring.ai.model.image=none",
        "spring.ai.model.audio.speech=none",
        "spring.ai.model.audio.transcription=none",
        "spring.ai.model.moderation=none",
        "spring.ai.openai.api-key=no-se-usa-en-tests",
        "asistente.rag.umbral-similitud=0.20",       // los embeddings falsos son léxicos: umbral acorde
})
@Import(ModelosIaFalsos.class)
@Testcontainers
@AutoConfigureObservability
class AsistenteApiIT {

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.1");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static final ObjectMapper json = new ObjectMapper();

    @Autowired WebTestClient cliente;

    @BeforeEach
    void esperarCorpusIndexado() {
        esperar(Duration.ofSeconds(30), () -> cliente.get().uri("/actuator/health/readiness").exchange()
                .returnResult(String.class).getStatus().is2xxSuccessful());
    }

    @AfterEach
    void restaurarProveedor() {
        ChatModelFalso.MODO.set(Modo.NORMAL);
    }

    private JsonNode consultar(String pregunta, int estadoEsperado) {
        byte[] cuerpo = cliente.post().uri("/asistente/consulta")
                .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                .header("Content-Type", "application/json")
                .bodyValue("{\"pregunta\":\"" + pregunta + "\"}")
                .exchange()
                .expectStatus().isEqualTo(estadoEsperado)
                .expectBody().returnResult().getResponseBody();
        return leer(cuerpo);
    }

    private static JsonNode leer(byte[] bytes) {
        try {
            return json.readTree(bytes);
        } catch (Exception e) {
            throw new AssertionError("Respuesta no es JSON válido", e);
        }
    }

    private static void esperar(Duration maximo, java.util.function.BooleanSupplier condicion) {
        Instant limite = Instant.now().plus(maximo);
        while (!condicion.getAsBoolean()) {
            if (Instant.now().isAfter(limite)) {
                throw new AssertionError("Se agotó el tiempo esperando la condición");
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    @Nested
    @DisplayName("RF-04 A: consulta con RAG y RN-05")
    class ConsultaRag {

        @Test
        void respondeCrisisHipertensivaCitandoProto001() {
            JsonNode r = consultar("¿Cuáles son las medidas iniciales ante una crisis hipertensiva?", 200);

            assertThat(r.get("respuesta").asText()).contains("Reconfirmar");
            assertThat(r.get("fuentes")).hasSize(1);
            assertThat(r.get("fuentes").get(0).get("protocolo").asText()).isEqualTo("PROTO-001");
            assertThat(r.get("fuentes").get(0).get("seccion").asText()).isEqualTo("Medidas iniciales");
        }

        @Test
        void respondeSepsisCitandoProto002() {
            JsonNode r = consultar("¿Cuándo se administra el primer antibiótico en sepsis?", 200);

            assertThat(r.get("respuesta").asText()).contains("primera hora");
            assertThat(r.get("fuentes").get(0).get("protocolo").asText()).isEqualTo("PROTO-002");
            assertThat(r.get("fuentes").get(0).get("seccion").asText()).isEqualTo("Acciones iniciales");
        }

        @Test
        void sinProtocoloEnElCorpusNoGeneraContenidoClinico() {
            JsonNode r = consultar("¿Cuál es el tratamiento de primera línea para la diabetes tipo 2?", 200);

            assertThat(r.get("respuesta").asText()).isEqualTo(RespuestaAsistente.AVISO_SIN_PROTOCOLO);
            assertThat(r.get("fuentes")).isEmpty();
        }

        @Test
        void unaCitaInventadaPorElModeloSeDescarta() {
            ChatModelFalso.MODO.set(Modo.ALUCINA);

            JsonNode r = consultar("¿Cuáles son las medidas iniciales ante una crisis hipertensiva?", 200);

            assertThat(r.get("respuesta").asText()).isEqualTo(RespuestaAsistente.AVISO_SIN_PROTOCOLO);
            assertThat(r.get("respuesta").asText()).doesNotContain("Metformina");
            assertThat(r.get("fuentes")).isEmpty();
        }

        @Test
        void conElProveedorCaidoRespondeModoDegradado503() {
            ChatModelFalso.MODO.set(Modo.CAIDO);

            JsonNode r = consultar("¿Cuáles son las medidas iniciales ante una crisis hipertensiva?", 503);

            assertThat(r.get("mensaje").asText()).contains("modo degradado").contains("proveedor de IA");
        }

        @Test
        void preguntaVaciaResponde400() {
            consultar("", 400);
        }
    }

    @Nested
    @DisplayName("RF-04 B: estado de urgencias desde triaje.eventos")
    class EstadoUrgencias {

        @Test
        void reconstruyeActivosPorNivelDeFormaIdempotente() {
            String p1 = UUID.randomUUID().toString();
            String p2 = UUID.randomUUID().toString();
            String p3 = UUID.randomUUID().toString();

            try (KafkaProducer<String, String> productor = new KafkaProducer<>(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
                publicar(productor, p1, "paciente_registrado", "CRITICO", "REGISTRADO");
                publicar(productor, p1, "paciente_registrado", "CRITICO", "REGISTRADO"); // duplicado (at-least-once)
                publicar(productor, p2, "paciente_registrado", "URGENTE", "REGISTRADO");
                publicar(productor, p2, "paciente_en_consulta", "URGENTE", "EN_CONSULTA");
                publicar(productor, p2, "paciente_alta", "URGENTE", "ALTA");
                publicar(productor, p3, "paciente_registrado", "NO_URGENTE", "REGISTRADO");
                publicar(productor, p3, "paciente_escalado", "NO_URGENTE", "REGISTRADO");
                publicar(productor, p2, "paciente_alta", "URGENTE", "ALTA");                // alta repetida
                productor.flush();
            }

            esperar(Duration.ofSeconds(30), () -> estado().get("activosPorNivel").get("NO_URGENTE").asLong() >= 1);

            JsonNode estado = estado();
            assertThat(estado.get("activosPorNivel").get("CRITICO").asLong()).isEqualTo(1);
            assertThat(estado.get("activosPorNivel").get("URGENTE").asLong()).isEqualTo(0);
            assertThat(estado.get("activosPorNivel").get("NO_URGENTE").asLong()).isEqualTo(1);
            assertThat(estado.get("totalActivos").asLong()).isEqualTo(2);
            assertThat(estado.get("activosPorNivel").size()).isEqualTo(5);
        }

        private JsonNode estado() {
            byte[] cuerpo = cliente.get().uri("/asistente/estado-urgencias")
                    .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                    .exchange().expectStatus().isOk()
                    .expectBody().returnResult().getResponseBody();
            return leer(cuerpo);
        }

        private static void publicar(KafkaProducer<String, String> productor, String pacienteId, String tipo,
                                     String nivel, String estado) {
            String evento = """
                    {"eventoId":"%s","tipo":"%s","version":1,"pacienteId":"%s","nivel":"%s","estado":"%s",
                     "escalado":false,"ocurridoEn":"%s"}""".formatted(UUID.randomUUID(), tipo, pacienteId, nivel, estado, Instant.now());
            productor.send(new ProducerRecord<>("triaje.eventos", pacienteId, evento));
        }
    }

    @Nested
    @DisplayName("Seguridad y observabilidad")
    class Transversales {

        @Test
        void sinApiKeyResponde401() {
            cliente.post().uri("/asistente/consulta").header("Content-Type", "application/json")
                    .bodyValue("{\"pregunta\":\"hola\"}")
                    .exchange().expectStatus().isUnauthorized();
        }

        @Test
        void exponeMetricasDeLlmYConsultas() {
            consultar("¿Cuándo se administra el primer antibiótico en sepsis?", 200);

            cliente.get().uri("/actuator/prometheus").exchange().expectStatus().isOk()
                    .expectBody(String.class).value(cuerpo -> assertThat(cuerpo)
                            .contains("asistente_llm_latencia")
                            .contains("asistente_consultas_total"));
        }

        @Test
        void elReadinessIncluyeElCorpusYRedis() {
            cliente.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk()
                    .expectBody().jsonPath("$.components.corpus.status").isEqualTo("UP")
                    .jsonPath("$.components.redis.status").isEqualTo("UP");
        }
    }
}
