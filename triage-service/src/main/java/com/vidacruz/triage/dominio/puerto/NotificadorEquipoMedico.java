package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.Paciente;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Puerto de salida: alerta al equipo médico cuando un paciente es escalado (RF-06).
 * El canal concreto (webhook, correo, etc.) es una decisión de infraestructura (ver ADR-004).
 */
public interface NotificadorEquipoMedico {

    Mono<Void> notificarEscalamiento(Paciente paciente, Duration tiempoEsperando);
}
