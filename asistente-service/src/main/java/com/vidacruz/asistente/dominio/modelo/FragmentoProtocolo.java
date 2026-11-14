package com.vidacruz.asistente.dominio.modelo;

/**
 * Sección de un protocolo del corpus oficial, tal como la devuelve la búsqueda semántica.
 *
 * @param protocolo código del protocolo (p. ej. PROTO-001)
 * @param titulo    título del protocolo
 * @param seccion   nombre de la sección (p. ej. "Medidas iniciales")
 * @param contenido texto de la sección
 * @param puntaje   similitud con la pregunta, en [0,1]
 */
public record FragmentoProtocolo(String protocolo, String titulo, String seccion, String contenido, double puntaje) {

    public Fuente fuente() {
        return new Fuente(protocolo, seccion);
    }
}
