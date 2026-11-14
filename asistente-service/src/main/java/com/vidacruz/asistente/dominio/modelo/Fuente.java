package com.vidacruz.asistente.dominio.modelo;

import java.util.Locale;

public record Fuente(String protocolo, String seccion) {
    public boolean esLaMismaQue(Fuente otra) {
        return normalizar(protocolo).equals(normalizar(otra.protocolo))
                && normalizar(seccion).equals(normalizar(otra.seccion));
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
