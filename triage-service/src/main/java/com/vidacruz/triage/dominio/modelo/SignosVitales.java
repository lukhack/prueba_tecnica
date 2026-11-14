package com.vidacruz.triage.dominio.modelo;

import com.vidacruz.triage.dominio.excepcion.ExcepcionValorInvalido;

import java.math.BigDecimal;

public record SignosVitales(
        int frecuenciaCardiaca,
        int presionSistolica,
        int presionDiastolica,
        int spo2,
        BigDecimal temperatura,
        int dolor,
        int gcs) {
    public SignosVitales {
        validarRango("frecuenciaCardiaca", frecuenciaCardiaca, 20, 250, "bpm");
        validarRango("presionSistolica", presionSistolica, 60, 260, "mmHg");
        validarRango("presionDiastolica", presionDiastolica, 30, 160, "mmHg");
        if (presionDiastolica >= presionSistolica) {
            throw new ExcepcionValorInvalido("presionDiastolica",
                    "La presión diastólica (" + presionDiastolica + ") debe ser menor que la sistólica (" + presionSistolica + ")");
        }
        validarRango("spo2", spo2, 70, 100, "%");
        if (temperatura == null) {
            throw new ExcepcionValorInvalido("temperatura", "La temperatura es obligatoria");
        }
        if (temperatura.compareTo(new BigDecimal("34.0")) < 0 || temperatura.compareTo(new BigDecimal("42.0")) > 0) {
            throw new ExcepcionValorInvalido("temperatura",
                    "La temperatura debe estar entre 34.0 y 42.0 °C (recibido: " + temperatura + ")");
        }
        validarRango("dolor", dolor, 0, 10, "");
        validarRango("gcs", gcs, 3, 15, "");
    }

    private static void validarRango(String campo, int valor, int minimo, int maximo, String unidad) {
        if (valor < minimo || valor > maximo) {
            String sufijo = unidad.isBlank() ? "" : " " + unidad;
            throw new ExcepcionValorInvalido(campo,
                    "El campo " + campo + " debe estar entre " + minimo + " y " + maximo + sufijo + " (recibido: " + valor + ")");
        }
    }

    public boolean temperaturaMayorOIgual(String valor) {
        return temperatura.compareTo(new BigDecimal(valor)) >= 0;
    }

    public boolean temperaturaMenor(String valor) {
        return temperatura.compareTo(new BigDecimal(valor)) < 0;
    }
}
