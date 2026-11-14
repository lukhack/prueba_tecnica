package com.vidacruz.triage.dominio.servicio;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.modelo.SignosVitales;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class ClasificadorTriaje {
    static final List<String> QUEJAS_ADMINISTRATIVAS = List.of(
            "documentos",
            "recoleccion de resultados",
            "renovacion de recetario",
            "vacunacion",
            "seguimiento");

    public NivelTriaje clasificar(SignosVitales s, String quejaPrincipal) {
        if (esCritico(s)) {
            return NivelTriaje.CRITICO;
        }
        if (esUrgente(s)) {
            return NivelTriaje.URGENTE;
        }
        if (esUrgente2(s)) {
            return NivelTriaje.URGENTE_2;
        }
        return esQuejaAdministrativa(quejaPrincipal) ? NivelTriaje.NO_URGENTE_2 : NivelTriaje.NO_URGENTE;
    }

    private boolean esCritico(SignosVitales s) {
        return s.frecuenciaCardiaca() < 40 || s.frecuenciaCardiaca() > 120
                || s.spo2() < 90
                || s.presionSistolica() < 90
                || s.gcs() <= 8;
    }

    private boolean esUrgente(SignosVitales s) {
        return entre(s.spo2(), 90, 93)
                || entre(s.presionSistolica(), 90, 100)
                || s.temperaturaMayorOIgual("38.5")
                || entre(s.gcs(), 9, 13);
    }

    private boolean esUrgente2(SignosVitales s) {
        return entre(s.frecuenciaCardiaca(), 90, 120)
                || (s.temperaturaMayorOIgual("38.0") && s.temperaturaMenor("38.5"))
                || s.dolor() >= 7;
    }

    private static boolean entre(int valor, int minimo, int maximo) {
        return valor >= minimo && valor <= maximo;
    }

    static boolean esQuejaAdministrativa(String quejaPrincipal) {
        if (quejaPrincipal == null) {
            return false;
        }
        String queja = normalizar(quejaPrincipal);
        return QUEJAS_ADMINISTRATIVAS.stream().anyMatch(queja::contains);
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}
