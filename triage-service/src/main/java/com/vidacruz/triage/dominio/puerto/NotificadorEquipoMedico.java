package com.vidacruz.triage.dominio.puerto;

import com.vidacruz.triage.dominio.modelo.Paciente;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface NotificadorEquipoMedico {
    Mono<Void> notificarEscalamiento(Paciente paciente, Duration tiempoEsperando);
}
