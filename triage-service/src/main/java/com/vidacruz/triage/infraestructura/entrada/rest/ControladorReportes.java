package com.vidacruz.triage.infraestructura.entrada.rest;

import com.vidacruz.triage.aplicacion.manejador.ManejadorReporteOperacion;
import com.vidacruz.triage.aplicacion.respuesta.RespuestaReporteOperacion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/triage/reportes")
public class ControladorReportes {
    private final ManejadorReporteOperacion reporte;

    public ControladorReportes(ManejadorReporteOperacion reporte) {
        this.reporte = reporte;
    }

    @GetMapping("/operacion")
    public Mono<RespuestaReporteOperacion> operacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        return reporte.ejecutar(desde, hasta);
    }
}
