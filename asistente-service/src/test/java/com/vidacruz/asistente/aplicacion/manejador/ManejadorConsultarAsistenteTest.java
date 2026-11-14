package com.vidacruz.asistente.aplicacion.manejador;

import com.vidacruz.asistente.dominio.excepcion.ExcepcionPreguntaInvalida;
import com.vidacruz.asistente.dominio.excepcion.ExcepcionProveedorIaNoDisponible;
import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.modelo.Fuente;
import com.vidacruz.asistente.dominio.modelo.RespuestaAsistente;
import com.vidacruz.asistente.dominio.modelo.RespuestaGenerada;
import com.vidacruz.asistente.dominio.puerto.BuscadorProtocolos;
import com.vidacruz.asistente.dominio.puerto.GeneradorRespuestas;
import com.vidacruz.asistente.dominio.puerto.MetricasAsistente;
import com.vidacruz.asistente.dominio.puerto.MetricasAsistente.ResultadoConsulta;
import com.vidacruz.asistente.dominio.servicio.PoliticaAntiAlucinacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManejadorConsultarAsistenteTest {
    private static final FragmentoProtocolo MEDIDAS_001 = new FragmentoProtocolo(
            "PROTO-001", "Crisis Hipertensiva, manejo inicial", "Medidas iniciales", "Reconfirmar...", 0.80);
    private static final FragmentoProtocolo LEJANO_003 = new FragmentoProtocolo(
            "PROTO-003", "Deshidratación pediátrica", "Rehidratación", "Leve...", 0.10);

    @Mock BuscadorProtocolos buscador;
    @Mock GeneradorRespuestas generador;
    @Mock MetricasAsistente metricas;

    private ManejadorConsultarAsistente manejador;

    @BeforeEach
    void setUp() {
        manejador = new ManejadorConsultarAsistente(buscador, generador, new PoliticaAntiAlucinacion(0.30), metricas, 4);
    }

    @Test
    void respondeConFuentesCuandoHayContextoYElModeloCitaCorrectamente() {
        when(buscador.buscar(anyString(), anyInt())).thenReturn(Mono.just(List.of(MEDIDAS_001, LEJANO_003)));
        when(generador.generar(anyString(), any())).thenReturn(Mono.just(new RespuestaGenerada(
                "Reconfirmar la presión tras 5 minutos de reposo.", List.of(new Fuente("PROTO-001", "Medidas iniciales")), false)));

        StepVerifier.create(manejador.ejecutar("¿Cuáles son las medidas iniciales ante una crisis hipertensiva?"))
                .assertNext(r -> {
                    assertThat(r.fuentes()).containsExactly(new Fuente("PROTO-001", "Medidas iniciales"));
                    assertThat(r.respuesta()).contains("Reconfirmar");
                })
                .verifyComplete();

        verify(generador).generar(anyString(), org.mockito.ArgumentMatchers.eq(List.of(MEDIDAS_001)));
        verify(metricas).registrarConsulta(ResultadoConsulta.CON_FUENTES);
    }

    @Test
    void sinFragmentosRelevantesNoLlamaAlModeloYAvisaQueNoHayProtocolo() {
        when(buscador.buscar(anyString(), anyInt())).thenReturn(Mono.just(List.of(LEJANO_003)));

        StepVerifier.create(manejador.ejecutar("¿Cuál es el tratamiento de primera línea para la diabetes tipo 2?"))
                .assertNext(r -> {
                    assertThat(r.respuesta()).isEqualTo(RespuestaAsistente.AVISO_SIN_PROTOCOLO);
                    assertThat(r.fuentes()).isEmpty();
                })
                .verifyComplete();

        verify(generador, never()).generar(anyString(), any());
        verify(metricas).registrarConsulta(ResultadoConsulta.SIN_PROTOCOLO);
    }

    @Test
    void propagaElModoDegradadoCuandoElProveedorFalla() {
        when(buscador.buscar(anyString(), anyInt())).thenReturn(Mono.just(List.of(MEDIDAS_001)));
        when(generador.generar(anyString(), any()))
                .thenReturn(Mono.error(new ExcepcionProveedorIaNoDisponible("timeout", null)));

        StepVerifier.create(manejador.ejecutar("¿Medidas ante crisis hipertensiva?"))
                .expectError(ExcepcionProveedorIaNoDisponible.class)
                .verify();

        verify(metricas).registrarConsulta(ResultadoConsulta.DEGRADADO);
    }

    @Test
    void rechazaPreguntasVacias() {
        StepVerifier.create(manejador.ejecutar("   "))
                .expectError(ExcepcionPreguntaInvalida.class)
                .verify();

        verify(buscador, never()).buscar(anyString(), anyInt());
    }
}
