package com.vidacruz.triage.dominio.excepcion;

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
