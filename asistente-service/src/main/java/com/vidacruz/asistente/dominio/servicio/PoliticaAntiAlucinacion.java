package com.vidacruz.asistente.dominio.servicio;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.modelo.Fuente;
import com.vidacruz.asistente.dominio.modelo.RespuestaAsistente;
import com.vidacruz.asistente.dominio.modelo.RespuestaGenerada;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de dominio que hace cumplir la RN-05 en tres capas:
 * <ol>
 *   <li><b>Antes del LLM:</b> si ningún fragmento del corpus supera el umbral de similitud, no se consulta
 *       al modelo y se responde "no existe protocolo disponible".</li>
 *   <li><b>Decisión del LLM:</b> si el modelo declara que el contexto no cubre la pregunta, se respeta.</li>
 *   <li><b>Después del LLM:</b> solo se conservan las citas que existen realmente entre los fragmentos
 *       recuperados; si no queda ninguna válida, la respuesta se descarta por completo.</li>
 * </ol>
 */
public class PoliticaAntiAlucinacion {

    private final double umbralSimilitud;

    public PoliticaAntiAlucinacion(double umbralSimilitud) {
        if (umbralSimilitud < 0 || umbralSimilitud > 1) {
            throw new IllegalArgumentException("El umbral de similitud debe estar en [0,1]");
        }
        this.umbralSimilitud = umbralSimilitud;
    }

    public double umbralSimilitud() {
        return umbralSimilitud;
    }

    /** Capa 1: ¿hay evidencia suficiente en el corpus para siquiera preguntar al modelo? */
    public List<FragmentoProtocolo> filtrarRelevantes(List<FragmentoProtocolo> recuperados) {
        return recuperados.stream().filter(f -> f.puntaje() >= umbralSimilitud).toList();
    }

    /** Capas 2 y 3: valida la respuesta del modelo contra el contexto que se le entregó. */
    public RespuestaAsistente validar(RespuestaGenerada generada, List<FragmentoProtocolo> contexto) {
        if (generada == null || generada.sinProtocolo() || generada.respuesta() == null || generada.respuesta().isBlank()) {
            return RespuestaAsistente.sinProtocoloDisponible();
        }
        List<Fuente> validas = new ArrayList<>();
        for (Fuente citada : generada.fuentes()) {
            boolean existe = contexto.stream().anyMatch(f -> f.fuente().esLaMismaQue(citada));
            boolean repetida = validas.stream().anyMatch(v -> v.esLaMismaQue(citada));
            if (existe && !repetida) {
                validas.add(contexto.stream().filter(f -> f.fuente().esLaMismaQue(citada)).findFirst().get().fuente());
            }
        }
        if (validas.isEmpty()) {
            // El modelo citó algo que no está en el corpus (o no citó nada): respuesta inválida por RN-05.
            return RespuestaAsistente.sinProtocoloDisponible();
        }
        return RespuestaAsistente.sustentada(generada.respuesta().trim(), validas);
    }
}
