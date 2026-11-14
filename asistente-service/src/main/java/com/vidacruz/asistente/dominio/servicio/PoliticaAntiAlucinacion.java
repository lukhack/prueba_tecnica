package com.vidacruz.asistente.dominio.servicio;

import com.vidacruz.asistente.dominio.modelo.FragmentoProtocolo;
import com.vidacruz.asistente.dominio.modelo.Fuente;
import com.vidacruz.asistente.dominio.modelo.RespuestaAsistente;
import com.vidacruz.asistente.dominio.modelo.RespuestaGenerada;

import java.util.ArrayList;
import java.util.List;

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

    public List<FragmentoProtocolo> filtrarRelevantes(List<FragmentoProtocolo> recuperados) {
        return recuperados.stream().filter(f -> f.puntaje() >= umbralSimilitud).toList();
    }

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
            return RespuestaAsistente.sinProtocoloDisponible();
        }
        return RespuestaAsistente.sustentada(generada.respuesta().trim(), validas);
    }
}
