package com.vidacruz.triage.infraestructura.entrada.programado;

import com.vidacruz.triage.aplicacion.manejador.ManejadorEscalarPacientesVencidos;
import com.vidacruz.triage.infraestructura.configuracion.PropiedadesEscalamiento;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PlanificadorEscalamiento {
    private static final Logger log = LoggerFactory.getLogger(PlanificadorEscalamiento.class);

    private final ManejadorEscalarPacientesVencidos manejador;
    private final PropiedadesEscalamiento propiedades;
    private Disposable suscripcion;

    public PlanificadorEscalamiento(ManejadorEscalarPacientesVencidos manejador, PropiedadesEscalamiento propiedades) {
        this.manejador = manejador;
        this.propiedades = propiedades;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        if (!propiedades.habilitado()) {
            log.info("Monitor de SLA deshabilitado por configuración");
            return;
        }
        log.info("Monitor de SLA iniciado: evaluación cada {}", propiedades.intervalo());
        suscripcion = Flux.interval(propiedades.intervalo())
                .onBackpressureDrop()
                .concatMap(tick -> manejador.ejecutar()
                        .doOnNext(escalados -> {
                            if (escalados > 0) {
                                log.info("Ciclo de SLA: {} paciente(s) escalado(s)", escalados);
                            }
                        })
                        .onErrorResume(error -> {
                            log.error("Fallo en el ciclo de evaluación de SLA", error);
                            return Mono.empty();
                        }))
                .subscribe();
    }

    @PreDestroy
    public void detener() {
        if (suscripcion != null) {
            suscripcion.dispose();
        }
    }
}
