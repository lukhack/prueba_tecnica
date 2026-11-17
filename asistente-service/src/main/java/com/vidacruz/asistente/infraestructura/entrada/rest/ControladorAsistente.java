package com.vidacruz.asistente.infraestructura.entrada.rest;

import com.vidacruz.asistente.aplicacion.manejador.ManejadorConsultarAsistente;
import com.vidacruz.asistente.aplicacion.manejador.ManejadorConsultarEstadoUrgencias;
import com.vidacruz.asistente.aplicacion.respuesta.RespuestaConsulta;
import com.vidacruz.asistente.aplicacion.respuesta.RespuestaEstadoUrgencias;
import com.vidacruz.asistente.infraestructura.entrada.rest.dto.SolicitudConsulta;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/asistente")
public class ControladorAsistente {
    private final ManejadorConsultarAsistente consultar;
    private final ManejadorConsultarEstadoUrgencias estadoUrgencias;

    public ControladorAsistente(ManejadorConsultarAsistente consultar, ManejadorConsultarEstadoUrgencias estadoUrgencias) {
        this.consultar = consultar;
        this.estadoUrgencias = estadoUrgencias;
    }

    @PostMapping("/consulta")
    public Mono<RespuestaConsulta> consulta(@Valid @RequestBody SolicitudConsulta solicitud) {
        return consultar.ejecutar(solicitud.pregunta());
    }

    @GetMapping("/estado-urgencias")
    public Mono<RespuestaEstadoUrgencias> estadoUrgencias() {
        return estadoUrgencias.ejecutar();
    }
}
