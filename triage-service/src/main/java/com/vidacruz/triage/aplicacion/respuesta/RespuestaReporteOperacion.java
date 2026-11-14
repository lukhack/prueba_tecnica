package com.vidacruz.triage.aplicacion.respuesta;

import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.modelo.ResumenOperacionHora;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reporte de operación agrupado por hora (RF-05). */
public record RespuestaReporteOperacion(Instant desde, Instant hasta, List<FilaHora> horas) {

    public record FilaHora(
            Instant hora,
            long totalRegistrados,
            Map<String, Long> conteoPorNivel,
            Double esperaPromedioMinutos,
            Double esperaP95Minutos) {

        public static FilaHora de(ResumenOperacionHora r) {
            // Siempre se devuelven los 5 niveles (con 0 si no hubo pacientes) para facilitar el consumo.
            Map<String, Long> conteos = new LinkedHashMap<>();
            for (NivelTriaje nivel : NivelTriaje.values()) {
                conteos.put(nivel.name(), r.conteoPorNivel().getOrDefault(nivel, 0L));
            }
            return new FilaHora(r.hora(), r.totalRegistrados(), conteos, r.esperaPromedioMinutos(), r.esperaP95Minutos());
        }
    }
}
