package com.vidacruz.triage.dominio.modelo;

import com.vidacruz.triage.dominio.excepcion.ExcepcionValorInvalido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RF-01: validaciones de rangos de signos vitales")
class SignosVitalesTest {
    @Test
    void aceptaValoresDentroDeRango() {
        assertThatCode(() -> new SignosVitales(20, 260, 30, 70, new BigDecimal("34.0"), 0, 3)).doesNotThrowAnyException();
        assertThatCode(() -> new SignosVitales(250, 261 - 1, 160, 100, new BigDecimal("42.0"), 10, 15)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{6} fuera de rango → 400 indicando el campo")
    @CsvSource({
            " 19, 120, 80, 97, 36.5, 0, frecuenciaCardiaca",
            "251, 120, 80, 97, 36.5, 0, frecuenciaCardiaca",
            " 70,  59, 40, 97, 36.5, 0, presionSistolica",
            " 70, 261, 80, 97, 36.5, 0, presionSistolica",
            " 70, 120, 29, 97, 36.5, 0, presionDiastolica",
            " 70, 200,161, 97, 36.5, 0, presionDiastolica",
            " 70, 120, 80, 69, 36.5, 0, spo2",
            " 70, 120, 80,101, 36.5, 0, spo2",
            " 70, 120, 80, 97, 33.9, 0, temperatura",
            " 70, 120, 80, 97, 42.1, 0, temperatura",
            " 70, 120, 80, 97, 36.5, 11, dolor",
            " 70, 120, 80, 97, 36.5, -1, dolor",
    })
    void rechazaValoresFueraDeRangoIndicandoElCampo(int fc, int ps, int pd, int spo2, String temp, int dolor, String campo) {
        assertThatThrownBy(() -> new SignosVitales(fc, ps, pd, spo2, new BigDecimal(temp), dolor, 15))
                .isInstanceOf(ExcepcionValorInvalido.class)
                .extracting("campo").isEqualTo(campo);
    }

    @Test
    void rechazaGcsFueraDeRango() {
        assertThatThrownBy(() -> new SignosVitales(70, 120, 80, 97, new BigDecimal("36.5"), 0, 2))
                .isInstanceOf(ExcepcionValorInvalido.class).extracting("campo").isEqualTo("gcs");
        assertThatThrownBy(() -> new SignosVitales(70, 120, 80, 97, new BigDecimal("36.5"), 0, 16))
                .isInstanceOf(ExcepcionValorInvalido.class).extracting("campo").isEqualTo("gcs");
    }

    @Test
    void laDiastolicaDebeSerMenorQueLaSistolica() {
        assertThatThrownBy(() -> new SignosVitales(70, 100, 100, 97, new BigDecimal("36.5"), 0, 15))
                .isInstanceOf(ExcepcionValorInvalido.class)
                .hasMessageContaining("menor que la sistólica")
                .extracting("campo").isEqualTo("presionDiastolica");
    }
}
