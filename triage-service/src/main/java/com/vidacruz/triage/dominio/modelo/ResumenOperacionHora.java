package com.vidacruz.triage.dominio.modelo;

import java.time.Instant;
import java.util.Map;

/**
 * Una fila del reporte de operación (RF-05): métricas de una hora concreta.
 * Las esperas son en minutos y pueden ser nulas si en esa hora nadie entró a consulta.
 */
public record ResumenOperacionHora(
        Instant hora,
        long totalRegistrados,
        Map<NivelTriaje, Long> conteoPorNivel,
        Double esperaPromedioMinutos,
        Double esperaP95Minutos) {
}
