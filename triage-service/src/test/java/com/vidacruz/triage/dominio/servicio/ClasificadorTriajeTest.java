package com.vidacruz.triage.dominio.servicio;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.modelo.SignosVitales;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN-01: clasificación por nivel de triaje")
class ClasificadorTriajeTest {
    private final ClasificadorTriaje clasificador = new ClasificadorTriaje();

    @ParameterizedTest(name = "{0}: FC={1} PS={2} PD={3} SpO2={4} T={5} dolor={6} GCS={7} \"{8}\" → {9}")
    @CsvSource({
            "E1, 130, 180, 110, 97, 36.8, 8, 15, Dolor torácico,          CRITICO",
            "E2,  95, 150,  95, 91, 37.0, 4, 15, Dificultad para respirar, URGENTE",
            "E3,  88, 130,  85, 98, 38.2, 6, 15, Fiebre,                   URGENTE_2",
            "E4,  72, 120,  80, 99, 36.5, 2, 15, Herida por cortar,        NO_URGENTE",
            "E5,  70, 115,  75, 99, 36.4, 0, 15, Renovación de recetario,  NO_URGENTE_2",
            "E6,  35,  80,  50, 85, 35.9, 0,  6, Inconsciente,             CRITICO",
    })
    void clasificaLosPacientesDeReferencia(String caso, int fc, int ps, int pd, int spo2, String temp, int dolor,
                                           int gcs, String queja, NivelTriaje esperado) {
        SignosVitales signos = new SignosVitales(fc, ps, pd, spo2, new BigDecimal(temp), dolor, gcs);

        assertThat(clasificador.clasificar(signos, queja)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "límite: FC={0} PS={1} SpO2={2} T={3} dolor={4} GCS={5} → {6}")
    @CsvSource({
            " 39, 120, 97, 36.5, 0, 15, CRITICO",
            "121, 120, 97, 36.5, 0, 15, CRITICO",
            " 70, 120, 89, 36.5, 0, 15, CRITICO",
            " 70,  89, 97, 36.5, 0, 15, CRITICO",
            " 70, 120, 97, 36.5, 0,  8, CRITICO",
            " 70, 120, 93, 36.5, 0, 15, URGENTE",
            " 70, 100, 97, 36.5, 0, 15, URGENTE",
            " 70, 120, 97, 38.5, 0, 15, URGENTE",
            " 70, 120, 97, 36.5, 0, 13, URGENTE",
            " 90, 120, 97, 36.5, 0, 15, URGENTE_2",
            "120, 120, 97, 36.5, 0, 15, URGENTE_2",
            " 70, 120, 97, 38.0, 0, 15, URGENTE_2",
            " 70, 120, 97, 38.4, 0, 15, URGENTE_2",
            " 70, 120, 97, 36.5, 7, 15, URGENTE_2",
            " 70, 120, 97, 36.5, 6, 14, NO_URGENTE",
    })
    void respetaLosBordesDeCadaCondicion(int fc, int ps, int spo2, String temp, int dolor, int gcs, NivelTriaje esperado) {
        SignosVitales signos = new SignosVitales(fc, ps, 60, spo2, new BigDecimal(temp), dolor, gcs);

        assertThat(clasificador.clasificar(signos, "Consulta general")).isEqualTo(esperado);
    }

    @Test
    void cuandoSeCumplenCondicionesDeVariosNivelesRigeElMasGrave() {
        SignosVitales signos = new SignosVitales(95, 150, 95, 91, new BigDecimal("37.0"), 9, 15);

        assertThat(clasificador.clasificar(signos, "Dolor")).isEqualTo(NivelTriaje.URGENTE);
    }

    @Test
    void laQuejaAdministrativaSoloAplicaSiElPacienteEsNivel4() {
        SignosVitales critico = new SignosVitales(130, 180, 110, 97, new BigDecimal("36.8"), 0, 15);

        assertThat(clasificador.clasificar(critico, "Vacunación")).isEqualTo(NivelTriaje.CRITICO);
    }

    @ParameterizedTest
    @CsvSource({
            "documentos, true",
            "Recolección de resultados, true",
            "RENOVACIÓN DE RECETARIO, true",
            "vacunacion, true",
            "Seguimiento de control, true",
            "Dolor abdominal, false",
            ", false",
    })
    void reconoceQuejasAdministrativasSinImportarMayusculasNiTildes(String queja, boolean esperado) {
        assertThat(ClasificadorTriaje.esQuejaAdministrativa(queja)).isEqualTo(esperado);
    }
}
