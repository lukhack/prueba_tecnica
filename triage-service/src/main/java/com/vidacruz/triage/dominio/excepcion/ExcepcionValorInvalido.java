package com.vidacruz.triage.dominio.excepcion;

/** Un dato de entrada está fuera del rango permitido. Se traduce a HTTP 400 indicando el campo. */
public class ExcepcionValorInvalido extends RuntimeException {

    private final String campo;

    public ExcepcionValorInvalido(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
