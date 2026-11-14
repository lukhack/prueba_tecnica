package com.vidacruz.asistente.dominio.excepcion;

/** La pregunta no cumple los requisitos mínimos (vacía o demasiado larga). HTTP 400. */
public class ExcepcionPreguntaInvalida extends RuntimeException {

    public ExcepcionPreguntaInvalida(String mensaje) {
        super(mensaje);
    }
}
