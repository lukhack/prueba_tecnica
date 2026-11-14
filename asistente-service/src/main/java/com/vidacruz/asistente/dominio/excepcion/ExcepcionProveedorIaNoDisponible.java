package com.vidacruz.asistente.dominio.excepcion;

public class ExcepcionProveedorIaNoDisponible extends RuntimeException {
    public ExcepcionProveedorIaNoDisponible(String detalle, Throwable causa) {
        super("El asistente clínico está en modo degradado: el proveedor de IA no está disponible (" + detalle
                + "). Los protocolos siguen disponibles en el corpus oficial; intente nuevamente en unos minutos.", causa);
    }
}
