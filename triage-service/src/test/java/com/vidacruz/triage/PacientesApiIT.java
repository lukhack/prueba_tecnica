package com.vidacruz.triage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.triage.aplicacion.manejador.ManejadorEscalarPacientesVencidos;
import com.vidacruz.triage.infraestructura.entrada.rest.FiltroApiKey;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "seguridad.api-key=clave-de-prueba",
        "triage.escalamiento.habilitado=false",
        "triage.sla.critico=0s",
        "triage.alertas.webhook-url=",
})
@Testcontainers
@AutoConfigureObservability
class PacientesApiIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.1");

    static KafkaConsumer<String, String> consumidor;
    static final List<ConsumerRecord<String, String>> recibidos = new ArrayList<>();
    static final ObjectMapper json = new ObjectMapper();

    @Autowired WebTestClient cliente;
    @Autowired ManejadorEscalarPacientesVencidos escalamiento;

    @BeforeAll
    static void iniciarConsumidorDeEventos() {
        consumidor = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
        consumidor.subscribe(List.of("triaje.eventos"));
    }

    @AfterAll
    static void cerrarConsumidor() {
        consumidor.close();
    }

    private static final String E1 = cuerpo("Elena Ruiz", 130, 180, 110, 97, "36.8", 8, 15, "Dolor torácico");

    static String cuerpo(String nombre, int fc, int ps, int pd, int spo2, String temp, int dolor, int gcs, String queja) {
        return """
                {"nombreCompleto":"%s","edad":45,"frecuenciaCardiaca":%d,"presionSistolica":%d,"presionDiastolica":%d,
                 "spo2":%d,"temperatura":%s,"dolor":%d,"gcs":%d,"quejaPrincipal":"%s"}
                """.formatted(nombre, fc, ps, pd, spo2, temp, dolor, gcs, queja);
    }

    private JsonNode registrar(String cuerpo) {
        byte[] respuesta = cliente.post().uri("/triage/pacientes")
                .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                .header("Content-Type", "application/json")
                .bodyValue(cuerpo)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult().getResponseBody();
        return leer(respuesta);
    }

    private WebTestClient.ResponseSpec cambiarEstado(String id, String estado) {
        return cliente.patch().uri("/triage/pacientes/{id}/estado", id)
                .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                .header("Content-Type", "application/json")
                .bodyValue("{\"estado\":\"" + estado + "\"}")
                .exchange();
    }

    private JsonNode consultar(String id) {
        byte[] respuesta = cliente.get().uri("/triage/pacientes/{id}", id)
                .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                .exchange().expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        return leer(respuesta);
    }

    private JsonNode auditoriaDe(String id) {
        byte[] respuesta = cliente.get().uri("/triage/pacientes/{id}/auditoria", id)
                .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                .exchange().expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        return leer(respuesta);
    }

    private static JsonNode leer(byte[] bytes) {
        try {
            return json.readTree(bytes);
        } catch (Exception e) {
            throw new AssertionError("Respuesta no es JSON válido", e);
        }
    }

    private static List<JsonNode> eventosDe(String pacienteId, String tipo, Duration maximo) {
        Instant limite = Instant.now().plus(maximo);
        while (Instant.now().isBefore(limite)) {
            consumidor.poll(Duration.ofMillis(300)).forEach(recibidos::add);
            List<JsonNode> coincidencias = filtrar(r -> r.key().equals(pacienteId), tipo);
            if (!coincidencias.isEmpty() && !maximo.equals(Duration.ZERO)) {
                return coincidencias;
            }
        }
        return filtrar(r -> r.key().equals(pacienteId), tipo);
    }

    private static List<JsonNode> filtrar(Predicate<ConsumerRecord<String, String>> porClave, String tipo) {
        List<JsonNode> resultado = new ArrayList<>();
        for (ConsumerRecord<String, String> r : recibidos) {
            if (!porClave.test(r)) continue;
            JsonNode nodo = leer(r.value().getBytes());
            if (tipo.equals(nodo.get("tipo").asText())) {
                resultado.add(nodo);
            }
        }
        return resultado;
    }

    @Nested
    @DisplayName("RF-01: registro y clasificación")
    class Registro {
        @ParameterizedTest(name = "{0} → {9} (SLA {10} min)")
        @CsvSource({
                "E1, 130, 180, 110, 97, 36.8, 8, 15, Dolor torácico,          CRITICO,      0",
                "E2,  95, 150,  95, 91, 37.0, 4, 15, Dificultad para respirar, URGENTE,     10",
                "E3,  88, 130,  85, 98, 38.2, 6, 15, Fiebre,                   URGENTE_2,   30",
                "E4,  72, 120,  80, 99, 36.5, 2, 15, Herida por cortar,        NO_URGENTE,  60",
                "E5,  70, 115,  75, 99, 36.4, 0, 15, Renovación de recetario,  NO_URGENTE_2, 120",
                "E6,  35,  80,  50, 85, 35.9, 0,  6, Inconsciente,             CRITICO,      0",
        })
        void registraClasificaYPublicaElEvento(String caso, int fc, int ps, int pd, int spo2, String temp, int dolor,
                                               int gcs, String queja, String nivel, int slaMinutos) {
            JsonNode paciente = registrar(cuerpo("Paciente " + caso, fc, ps, pd, spo2, temp, dolor, gcs, queja));

            assertThat(paciente.get("id").asText()).isNotBlank();
            assertThat(paciente.get("nivel").asText()).isEqualTo(nivel);
            assertThat(paciente.get("slaMinutos").asLong()).isEqualTo(slaMinutos);
            assertThat(paciente.get("estado").asText()).isEqualTo("REGISTRADO");
            assertThat(Instant.parse(paciente.get("fechaRegistro").asText())).isBeforeOrEqualTo(Instant.now());

            List<JsonNode> eventos = eventosDe(paciente.get("id").asText(), "paciente_registrado", Duration.ofSeconds(20));
            assertThat(eventos).hasSize(1);
            assertThat(eventos.get(0).get("nivel").asText()).isEqualTo(nivel);
        }

        @Test
        void rechazaValoresFueraDeRangoIndicandoElCampo() {
            cliente.post().uri("/triage/pacientes")
                    .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                    .header("Content-Type", "application/json")
                    .bodyValue(cuerpo("Luis Gómez", 300, 120, 80, 97, "36.5", 0, 15, "Mareo"))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.campo").isEqualTo("frecuenciaCardiaca")
                    .jsonPath("$.mensaje").value(m -> assertThat((String) m).contains("20 y 250"));
        }

        @Test
        void rechazaCamposFaltantes() {
            cliente.post().uri("/triage/pacientes")
                    .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"nombreCompleto\":\"Sin signos\"}")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.campo").isNotEmpty();
        }
    }

    @Nested
    @DisplayName("RF-02: flujo de estados y auditoría")
    class FlujoDeEstados {
        @Test
        void recorreElFlujoCompletoYAuditaCadaTransicion() {
            String id = registrar(cuerpo("Marta López", 95, 150, 95, 91, "37.0", 4, 15, "Dificultad para respirar"))
                    .get("id").asText();

            cambiarEstado(id, "EN_CONSULTA").expectStatus().isOk()
                    .expectBody().jsonPath("$.estado").isEqualTo("EN_CONSULTA")
                    .jsonPath("$.fechaEntradaConsulta").isNotEmpty();
            cambiarEstado(id, "ALTA").expectStatus().isOk()
                    .expectBody().jsonPath("$.estado").isEqualTo("ALTA");

            JsonNode auditoria = auditoriaDe(id);
            assertThat(auditoria).extracting(n -> n.get("tipoEvento").asText())
                    .containsExactly("PACIENTE_REGISTRADO", "PACIENTE_EN_CONSULTA", "PACIENTE_ALTA");
            assertThat(auditoria.get(1).get("estadoAnterior").asText()).isEqualTo("REGISTRADO");
            assertThat(auditoria.get(2).get("estadoAnterior").asText()).isEqualTo("EN_CONSULTA");

            assertThat(eventosDe(id, "paciente_en_consulta", Duration.ofSeconds(20))).hasSize(1);
            assertThat(eventosDe(id, "paciente_alta", Duration.ofSeconds(20))).hasSize(1);
        }

        @Test
        void unaTransicionInvalidaResponde409SinAlterarAlPaciente() {
            String id = registrar(E1).get("id").asText();
            cambiarEstado(id, "ALTA").expectStatus().isOk();

            cambiarEstado(id, "EN_CONSULTA").expectStatus().isEqualTo(409)
                    .expectBody().jsonPath("$.mensaje").value(m -> assertThat((String) m).contains("ALTA").contains("EN_CONSULTA"));

            assertThat(consultar(id).get("estado").asText()).isEqualTo("ALTA");
            assertThat(auditoriaDe(id)).hasSize(2);
        }

        @Test
        void unEstadoDesconocidoResponde400() {
            String id = registrar(E1).get("id").asText();
            cambiarEstado(id, "HOSPITALIZADO").expectStatus().isBadRequest();
        }

        @Test
        void pacienteInexistenteResponde404() {
            cliente.get().uri("/triage/pacientes/{id}", UUID.randomUUID())
                    .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                    .exchange().expectStatus().isNotFound();
        }

        @Test
        void laConsultaIncluyeFlagsYTiempoEnElEstadoActual() {
            String id = registrar(E1).get("id").asText();

            JsonNode paciente = consultar(id);

            assertThat(paciente.get("escalado").asBoolean()).isFalse();
            assertThat(paciente.get("tiempoEnEstadoActualMinutos").asLong()).isGreaterThanOrEqualTo(0);
            assertThat(paciente.get("tiempoEnEstadoActual").asText()).startsWith("PT");
            assertThat(paciente.get("signosVitales").get("frecuenciaCardiaca").asInt()).isEqualTo(130);
        }
    }

    @Nested
    @DisplayName("RF-03 / RN-03: escalamiento idempotente")
    class Escalamiento {
        @Test
        void escalaUnaSolaVezAunqueElCicloCorraVariasVeces() {
            String id = registrar(E1).get("id").asText();

            Long primera = escalamiento.ejecutar().block(Duration.ofSeconds(30));
            Long segunda = escalamiento.ejecutar().block(Duration.ofSeconds(30));

            assertThat(primera).isGreaterThanOrEqualTo(1);
            JsonNode paciente = consultar(id);
            assertThat(paciente.get("escalado").asBoolean()).isTrue();
            assertThat(paciente.get("estado").asText()).isEqualTo("REGISTRADO");
            assertThat(paciente.get("fechaEscalamiento").asText()).isNotBlank();

            long escalamientosAuditados = 0;
            for (JsonNode n : auditoriaDe(id)) {
                if ("PACIENTE_ESCALADO".equals(n.get("tipoEvento").asText())) escalamientosAuditados++;
            }
            assertThat(escalamientosAuditados).isEqualTo(1);

            assertThat(eventosDe(id, "paciente_escalado", Duration.ofSeconds(20))).hasSize(1);

            eventosDe(id, "paciente_escalado", Duration.ofSeconds(2));
            assertThat(eventosDe(id, "paciente_escalado", Duration.ZERO)).hasSize(1);
            assertThat(segunda).isNotNull();
        }

        @Test
        void unPacienteEnConsultaNoSeEscalaAunqueHayaVencidoSuSla() {
            String id = registrar(E1).get("id").asText();
            cambiarEstado(id, "EN_CONSULTA").expectStatus().isOk();

            escalamiento.ejecutar().block(Duration.ofSeconds(30));

            assertThat(consultar(id).get("escalado").asBoolean()).isFalse();
        }
    }

    @Nested
    @DisplayName("RF-05: reporte de operación (SQL avanzado)")
    class Reporte {
        @Test
        void agrupaPorHoraConConteosPromedioYPercentil() {
            String enConsulta = registrar(cuerpo("Paciente Reporte 1", 88, 130, 85, 98, "38.2", 6, 15, "Fiebre")).get("id").asText();
            registrar(cuerpo("Paciente Reporte 2", 72, 120, 80, 99, "36.5", 2, 15, "Herida por cortar"));
            cambiarEstado(enConsulta, "EN_CONSULTA").expectStatus().isOk();

            Instant desde = Instant.now().minus(Duration.ofHours(1));
            Instant hasta = Instant.now().plus(Duration.ofHours(1));
            byte[] respuesta = cliente.get()
                    .uri(u -> u.path("/triage/reportes/operacion").queryParam("desde", desde).queryParam("hasta", hasta).build())
                    .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                    .exchange().expectStatus().isOk()
                    .expectBody().returnResult().getResponseBody();

            JsonNode reporte = leer(respuesta);
            assertThat(reporte.get("horas").size()).isBetween(1, 2);
            long total = 0;
            boolean hayEspera = false;
            for (JsonNode hora : reporte.get("horas")) {
                total += hora.get("totalRegistrados").asLong();
                assertThat(hora.get("conteoPorNivel").size()).isEqualTo(5);
                if (!hora.get("esperaPromedioMinutos").isNull()) {
                    hayEspera = true;
                    assertThat(hora.get("esperaP95Minutos").asDouble()).isGreaterThanOrEqualTo(0);
                }
            }
            assertThat(total).isGreaterThanOrEqualTo(2);
            assertThat(hayEspera).isTrue();
        }

        @Test
        void rechazaRangosInvalidos() {
            cliente.get()
                    .uri(u -> u.path("/triage/reportes/operacion")
                            .queryParam("desde", "2026-11-14T10:00:00Z").queryParam("hasta", "2026-11-14T09:00:00Z").build())
                    .header(FiltroApiKey.CABECERA, "clave-de-prueba")
                    .exchange().expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.campo").isEqualTo("hasta");
        }
    }

    @Nested
    @DisplayName("Seguridad y observabilidad")
    class Transversales {
        @Test
        void sinApiKeyResponde401() {
            cliente.get().uri("/triage/pacientes/{id}", UUID.randomUUID())
                    .exchange().expectStatus().isUnauthorized()
                    .expectBody().jsonPath("$.mensaje").value(m -> assertThat((String) m).contains("X-API-Key"));
        }

        @Test
        void losProbesYMetricasSonPublicos() {
            cliente.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();
            cliente.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk();
            cliente.get().uri("/actuator/prometheus").exchange().expectStatus().isOk()
                    .expectBody(String.class).value(cuerpo -> assertThat(cuerpo)
                            .contains("triage_clasificaciones_total")
                            .contains("triage_escalamientos_total"));
        }
    }
}
