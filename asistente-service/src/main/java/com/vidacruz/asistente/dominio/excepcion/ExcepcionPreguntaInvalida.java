package com.vidacruz.asistente.dominio.excepcion;

public class ExcepcionPreguntaInvalida extends RuntimeException {
    public ExcepcionPreguntaInvalida(String mensaje) {
        super(mensaje);
    }
}
