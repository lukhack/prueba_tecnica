package com.vidacruz.asistente.dominio.modelo;

import java.util.Locale;

/** Cita a una sección concreta del corpus oficial. */
public record Fuente(String protocolo, String seccion) {

    /** Dos fuentes son la misma sección aunque difieran en mayúsculas o espacios. */
    public boolean esLaMismaQue(Fuente otra) {
        return normalizar(protocolo).equals(normalizar(otra.protocolo))
                && normalizar(seccion).equals(normalizar(otra.seccion));
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
