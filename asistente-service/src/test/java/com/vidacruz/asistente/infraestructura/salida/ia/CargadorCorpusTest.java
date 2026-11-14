package com.vidacruz.asistente.infraestructura.salida.ia;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CargadorCorpusTest {
    @Test
    void parteUnProtocoloEnUnFragmentoPorSeccion() {
        String markdown = """
                # PROTO-001 — Crisis Hipertensiva, manejo inicial
                ## Definición
                Presión arterial ≥ 180/120 mmHg.
                ## Medidas iniciales
                Reconfirmar la presión tras 5 minutos de reposo.
                No reducir más de un 25% en la primera hora.
                """;

        List<FragmentoProtocolo> fragmentos = CargadorCorpus.parsear(markdown, "PROTO-001.md");

        assertThat(fragmentos).hasSize(2);
        assertThat(fragmentos.get(0).protocolo()).isEqualTo("PROTO-001");
        assertThat(fragmentos.get(0).titulo()).isEqualTo("Crisis Hipertensiva, manejo inicial");
        assertThat(fragmentos.get(0).seccion()).isEqualTo("Definición");
        assertThat(fragmentos.get(1).seccion()).isEqualTo("Medidas iniciales");
        assertThat(fragmentos.get(1).contenido()).contains("Reconfirmar").contains("25%");
    }

    @Test
    void cargaElCorpusOficialDesdeElClasspath() {
        List<FragmentoProtocolo> fragmentos = new CargadorCorpus().cargar();

        assertThat(fragmentos).extracting(FragmentoProtocolo::protocolo)
                .contains("PROTO-001", "PROTO-002", "PROTO-003");
        assertThat(fragmentos).anyMatch(f -> f.protocolo().equals("PROTO-002") && f.seccion().equals("Acciones iniciales"));
    }

    @Test
    void rechazaArchivosSinEncabezadoDeProtocolo() {
        assertThatThrownBy(() -> CargadorCorpus.parsear("## Sección huérfana\ntexto", "malo.md"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malo.md");
    }
}
