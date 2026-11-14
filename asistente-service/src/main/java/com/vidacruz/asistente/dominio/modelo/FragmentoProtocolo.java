package com.vidacruz.asistente.dominio.modelo;

public record FragmentoProtocolo(String protocolo, String titulo, String seccion, String contenido, double puntaje) {
    public Fuente fuente() {
        return new Fuente(protocolo, seccion);
    }
}
