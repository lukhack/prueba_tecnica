package com.vidacruz.triage.dominio.modelo;

import com.vidacruz.triage.dominio.excepcion.ExcepcionTransicionInvalida;
import com.vidacruz.triage.dominio.excepcion.ExcepcionValorInvalido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PacienteTest {
    private static final Instant T0 = Instant.parse("2026-11-14T10:00:00Z");
    private static final SignosVitales SIGNOS_NORMALES =
            new SignosVitales(72, 120, 80, 99, new BigDecimal("36.5"), 2, 15);

    private static Paciente pacienteRegistrado(NivelTriaje nivel) {
        return Paciente.registrar("Ana Pérez", 34, SIGNOS_NORMALES, "Herida por cortar", nivel, T0);
    }

    @Nested
    @DisplayName("RF-01: registro")
    class Registro {
        @Test
        void nacePendienteDeConsultaYSinEscalar() {
            Paciente p = pacienteRegistrado(NivelTriaje.NO_URGENTE);

            assertThat(p.id()).isNotNull();
            assertThat(p.estado()).isEqualTo(EstadoPaciente.REGISTRADO);
            assertThat(p.escalado()).isFalse();
            assertThat(p.fechaRegistro()).isEqualTo(T0);
            assertThat(p.fechaUltimoCambioEstado()).isEqualTo(T0);
            assertThat(p.fechaEntradaConsulta()).isNull();
        }

        @ParameterizedTest
        @CsvSource({"ab, 30, nombreCompleto", "Ana Pérez, -1, edad", "Ana Pérez, 121, edad"})
        void validaNombreYEdad(String nombre, int edad, String campo) {
            assertThatThrownBy(() -> Paciente.registrar(nombre, edad, SIGNOS_NORMALES, "Fiebre", NivelTriaje.NO_URGENTE, T0))
                    .isInstanceOf(ExcepcionValorInvalido.class)
                    .extracting("campo").isEqualTo(campo);
        }
    }

    @Nested
    @DisplayName("RF-02: flujo de estados")
    class FlujoDeEstados {
        @Test
        void registradoPuedePasarAConsultaYLuegoAAlta() {
            Paciente p = pacienteRegistrado(NivelTriaje.URGENTE);
            Instant t1 = T0.plus(Duration.ofMinutes(4));
            Instant t2 = T0.plus(Duration.ofMinutes(20));

            assertThat(p.cambiarEstado(EstadoPaciente.EN_CONSULTA, t1)).isEqualTo(EstadoPaciente.REGISTRADO);
            assertThat(p.fechaEntradaConsulta()).isEqualTo(t1);
            assertThat(p.cambiarEstado(EstadoPaciente.ALTA, t2)).isEqualTo(EstadoPaciente.EN_CONSULTA);
            assertThat(p.fechaAlta()).isEqualTo(t2);
            assertThat(p.tiempoEnEstadoActual(t2.plusSeconds(90))).isEqualTo(Duration.ofSeconds(90));
        }

        @Test
        void registradoPuedeRecibirAltaDirecta() {
            Paciente p = pacienteRegistrado(NivelTriaje.NO_URGENTE_2);

            p.cambiarEstado(EstadoPaciente.ALTA, T0.plusSeconds(60));

            assertThat(p.estado()).isEqualTo(EstadoPaciente.ALTA);
            assertThat(p.fechaEntradaConsulta()).isNull();
        }

        @ParameterizedTest(name = "{0} → {1} es inválida")
        @CsvSource({
                "ALTA, EN_CONSULTA",
                "ALTA, REGISTRADO",
                "EN_CONSULTA, REGISTRADO",
                "EN_CONSULTA, EN_CONSULTA",
                "REGISTRADO, REGISTRADO",
        })
        void rechazaTransicionesInvalidas(EstadoPaciente desde, EstadoPaciente hacia) {
            Paciente p = pacienteRegistrado(NivelTriaje.URGENTE);
            llevarA(p, desde);

            assertThatThrownBy(() -> p.cambiarEstado(hacia, T0.plusSeconds(600)))
                    .isInstanceOf(ExcepcionTransicionInvalida.class)
                    .hasMessageContaining(desde.name())
                    .hasMessageContaining(hacia.name());
            assertThat(p.estado()).isEqualTo(desde);
        }

        private void llevarA(Paciente p, EstadoPaciente destino) {
            if (destino == EstadoPaciente.EN_CONSULTA) {
                p.cambiarEstado(EstadoPaciente.EN_CONSULTA, T0.plusSeconds(1));
            } else if (destino == EstadoPaciente.ALTA) {
                p.cambiarEstado(EstadoPaciente.ALTA, T0.plusSeconds(1));
            }
        }
    }

    @Nested
    @DisplayName("RF-03 / RN-03: SLA y escalamiento idempotente")
    class Escalamiento {
        @Test
        void superaElSlaSoloCuandoPasaElTiempoDelNivel() {
            Paciente p = pacienteRegistrado(NivelTriaje.CRITICO);
            Duration sla = Duration.ofMinutes(5);

            assertThat(p.haSuperadoSla(sla, T0.plus(Duration.ofMinutes(4)))).isFalse();
            assertThat(p.haSuperadoSla(sla, T0.plus(Duration.ofMinutes(5)))).isTrue();
        }

        @Test
        void seEscalaUnaUnicaVez() {
            Paciente p = pacienteRegistrado(NivelTriaje.CRITICO);
            Instant t = T0.plus(Duration.ofMinutes(6));

            assertThat(p.escalar(t)).isTrue();
            assertThat(p.escalado()).isTrue();
            assertThat(p.fechaEscalamiento()).isEqualTo(t);

            assertThat(p.escalar(t.plusSeconds(30))).isFalse();
            assertThat(p.fechaEscalamiento()).isEqualTo(t);
            assertThat(p.haSuperadoSla(Duration.ofMinutes(5), t.plusSeconds(30))).isFalse();
        }

        @Test
        void unPacienteEnConsultaOConAltaNuncaSeEscala() {
            Paciente enConsulta = pacienteRegistrado(NivelTriaje.CRITICO);
            enConsulta.cambiarEstado(EstadoPaciente.EN_CONSULTA, T0.plusSeconds(10));

            Paciente conAlta = pacienteRegistrado(NivelTriaje.CRITICO);
            conAlta.cambiarEstado(EstadoPaciente.ALTA, T0.plusSeconds(10));

            Instant muchoDespues = T0.plus(Duration.ofHours(3));
            assertThat(enConsulta.haSuperadoSla(Duration.ofMinutes(5), muchoDespues)).isFalse();
            assertThat(enConsulta.escalar(muchoDespues)).isFalse();
            assertThat(conAlta.haSuperadoSla(Duration.ofMinutes(5), muchoDespues)).isFalse();
            assertThat(conAlta.escalar(muchoDespues)).isFalse();
        }

        @Test
        void elEscalamientoNoCambiaElEstadoClinico() {
            Paciente p = pacienteRegistrado(NivelTriaje.URGENTE);
            p.escalar(T0.plus(Duration.ofMinutes(11)));

            RegistroAuditoria auditoria = RegistroAuditoria.deEscalamiento(p, T0.plus(Duration.ofMinutes(11)));

            assertThat(p.estado()).isEqualTo(EstadoPaciente.REGISTRADO);
            assertThat(auditoria.tipoEvento()).isEqualTo(TipoEvento.PACIENTE_ESCALADO);
            assertThat(auditoria.estadoAnterior()).isEqualTo(EstadoPaciente.REGISTRADO);
            assertThat(auditoria.estadoNuevo()).isEqualTo(EstadoPaciente.REGISTRADO);
        }
    }
}
