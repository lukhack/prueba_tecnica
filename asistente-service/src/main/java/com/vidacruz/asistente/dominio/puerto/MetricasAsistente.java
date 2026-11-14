package com.vidacruz.asistente.dominio.puerto;

public interface MetricasAsistente {
    enum ResultadoConsulta { CON_FUENTES, SIN_PROTOCOLO, DEGRADADO }

    void registrarConsulta(ResultadoConsulta resultado);
}
