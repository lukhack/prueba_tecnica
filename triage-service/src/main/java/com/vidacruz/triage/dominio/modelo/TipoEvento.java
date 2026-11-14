package com.vidacruz.triage.dominio.modelo;

public enum TipoEvento {
    PACIENTE_REGISTRADO("paciente_registrado"),
    PACIENTE_EN_CONSULTA("paciente_en_consulta"),
    PACIENTE_ALTA("paciente_alta"),
    PACIENTE_ESCALADO("paciente_escalado");

    private final String nombreEvento;

    TipoEvento(String nombreEvento) {
        this.nombreEvento = nombreEvento;
    }

    public String nombreEvento() {
        return nombreEvento;
    }

    public static TipoEvento paraTransicionA(EstadoPaciente nuevoEstado) {
        return switch (nuevoEstado) {
            case REGISTRADO -> PACIENTE_REGISTRADO;
            case EN_CONSULTA -> PACIENTE_EN_CONSULTA;
            case ALTA -> PACIENTE_ALTA;
        };
    }
}
