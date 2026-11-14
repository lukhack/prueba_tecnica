package com.vidacruz.triage.aplicacion.respuesta;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;
import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.modelo.Paciente;
import com.vidacruz.triage.dominio.modelo.SignosVitales;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record RespuestaPaciente(
        UUID id,
        String nombreCompleto,
        int edad,
        SignosVitales signosVitales,
        String quejaPrincipal,
        NivelTriaje nivel,
        long slaMinutos,
        EstadoPaciente estado,
        boolean escalado,
        Instant fechaRegistro,
        Instant fechaUltimoCambioEstado,
        Instant fechaEntradaConsulta,
        Instant fechaAlta,
        Instant fechaEscalamiento,
        long tiempoEnEstadoActualMinutos,
        Duration tiempoEnEstadoActual) {
    public static RespuestaPaciente de(Paciente p, long slaMinutos, Instant ahora) {
        Duration enEstado = p.tiempoEnEstadoActual(ahora);
        return new RespuestaPaciente(
                p.id(), p.nombreCompleto(), p.edad(), p.signosVitales(), p.quejaPrincipal(),
                p.nivel(), slaMinutos, p.estado(), p.escalado(),
                p.fechaRegistro(), p.fechaUltimoCambioEstado(), p.fechaEntradaConsulta(), p.fechaAlta(),
                p.fechaEscalamiento(), enEstado.toMinutes(), enEstado);
    }
}
