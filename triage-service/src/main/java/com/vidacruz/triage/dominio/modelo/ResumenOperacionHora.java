package com.vidacruz.triage.dominio.modelo;

import java.time.Instant;
import java.util.Map;

public record ResumenOperacionHora(
        Instant hora,
        long totalRegistrados,
        Map<NivelTriaje, Long> conteoPorNivel,
        Double esperaPromedioMinutos,
        Double esperaP95Minutos) {
}
