package com.vidacruz.asistente.dominio.servicio;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.modelo.Fuente;
import com.vidacruz.asistente.dominio.modelo.RespuestaAsistente;
import com.vidacruz.asistente.dominio.modelo.RespuestaGenerada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN-05: política anti-alucinación")
class PoliticaAntiAlucinacionTest {
    private static final FragmentoProtocolo MEDIDAS_001 = new FragmentoProtocolo(
            "PROTO-001", "Crisis Hipertensiva, manejo inicial", "Medidas iniciales", "Reconfirmar la presión...", 0.82);
    private static final FragmentoProtocolo CRITERIOS_002 = new FragmentoProtocolo(
            "PROTO-002", "Sepsis, identificación temprana (qSOFA)", "Criterios", "2 o más de...", 0.41);
    private static final FragmentoProtocolo IRRELEVANTE_003 = new FragmentoProtocolo(
            "PROTO-003", "Deshidratación pediátrica", "Rehidratación", "Leve o moderada...", 0.12);

    private final PoliticaAntiAlucinacion politica = new PoliticaAntiAlucinacion(0.30);

    @Test
    void filtraLosFragmentosPorDebajoDelUmbral() {
        List<FragmentoProtocolo> relevantes = politica.filtrarRelevantes(List.of(MEDIDAS_001, CRITERIOS_002, IRRELEVANTE_003));

        assertThat(relevantes).containsExactly(MEDIDAS_001, CRITERIOS_002);
    }

    @Test
    void aceptaUnaRespuestaCuyasCitasExistenEnElContexto() {
        RespuestaGenerada generada = new RespuestaGenerada("Reconfirmar la presión tras 5 minutos de reposo.",
                List.of(new Fuente("proto-001", "medidas iniciales ")), false);

        RespuestaAsistente respuesta = politica.validar(generada, List.of(MEDIDAS_001, CRITERIOS_002));

        assertThat(respuesta.tieneFuentes()).isTrue();
        assertThat(respuesta.fuentes()).containsExactly(new Fuente("PROTO-001", "Medidas iniciales"));
        assertThat(respuesta.respuesta()).startsWith("Reconfirmar");
    }

    @Test
    void descartaCitasInventadasYConservaLasValidas() {
        RespuestaGenerada generada = new RespuestaGenerada("Texto",
                List.of(new Fuente("PROTO-999", "Tratamiento"), new Fuente("PROTO-001", "Medidas iniciales"),
                        new Fuente("PROTO-001", "Medidas iniciales")), false);

        RespuestaAsistente respuesta = politica.validar(generada, List.of(MEDIDAS_001));

        assertThat(respuesta.fuentes()).containsExactly(new Fuente("PROTO-001", "Medidas iniciales"));
    }

    @Test
    void siTodasLasCitasSonInventadasLaRespuestaEsInvalida() {
        RespuestaGenerada generada = new RespuestaGenerada("Metformina como primera línea.",
                List.of(new Fuente("PROTO-777", "Diabetes")), false);

        RespuestaAsistente respuesta = politica.validar(generada, List.of(MEDIDAS_001));

        assertThat(respuesta).isEqualTo(RespuestaAsistente.sinProtocoloDisponible());
        assertThat(respuesta.respuesta()).doesNotContain("Metformina");
    }

    @Test
    void siElModeloNoCitaNadaNoHayContenidoClinico() {
        RespuestaGenerada generada = new RespuestaGenerada("Contenido sin respaldo", List.of(), false);

        assertThat(politica.validar(generada, List.of(MEDIDAS_001))).isEqualTo(RespuestaAsistente.sinProtocoloDisponible());
    }

    @Test
    void respetaCuandoElModeloDeclaraQueNoHayProtocolo() {
        RespuestaGenerada generada = new RespuestaGenerada("", List.of(), true);

        assertThat(politica.validar(generada, List.of(MEDIDAS_001))).isEqualTo(RespuestaAsistente.sinProtocoloDisponible());
    }
}
