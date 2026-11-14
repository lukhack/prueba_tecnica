package com.vidacruz.asistente.dominio.puerto;

/** Puerto de salida: métricas de negocio del asistente. */
public interface MetricasAsistente {

    enum ResultadoConsulta { CON_FUENTES, SIN_PROTOCOLO, DEGRADADO }

    void registrarConsulta(ResultadoConsulta resultado);
}
