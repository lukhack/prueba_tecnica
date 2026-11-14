package com.vidacruz.asistente.dominio.excepcion;

/** El proveedor de IA no respondió (timeout, error o circuit breaker abierto). Modo degradado → HTTP 503. */
public class ExcepcionProveedorIaNoDisponible extends RuntimeException {

    public ExcepcionProveedorIaNoDisponible(String detalle, Throwable causa) {
        super("El asistente clínico está en modo degradado: el proveedor de IA no está disponible (" + detalle
                + "). Los protocolos siguen disponibles en el corpus oficial; intente nuevamente en unos minutos.", causa);
    }
}
