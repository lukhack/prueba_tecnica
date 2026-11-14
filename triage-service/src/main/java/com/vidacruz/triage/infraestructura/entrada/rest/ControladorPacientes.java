package com.vidacruz.triage.infraestructura.entrada.rest;

import com.vidacruz.triage.aplicacion.comando.ComandoCambiarEstado;
import com.vidacruz.triage.aplicacion.manejador.ManejadorCambiarEstado;
import com.vidacruz.triage.aplicacion.manejador.ManejadorConsultarPaciente;
import com.vidacruz.triage.aplicacion.manejador.ManejadorRegistrarPaciente;
import com.vidacruz.triage.aplicacion.respuesta.RespuestaPaciente;
import com.vidacruz.triage.dominio.modelo.RegistroAuditoria;
import com.vidacruz.triage.dominio.puerto.RepositorioAuditoria;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Adaptador de entrada REST para RF-01 y RF-02. */
@RestController
@RequestMapping("/triage/pacientes")
public class ControladorPacientes {

    private final ManejadorRegistrarPaciente registrar;
    private final ManejadorCambiarEstado cambiarEstado;
    private final ManejadorConsultarPaciente consultar;
    private final RepositorioAuditoria auditoria;

    public ControladorPacientes(ManejadorRegistrarPaciente registrar, ManejadorCambiarEstado cambiarEstado,
                                ManejadorConsultarPaciente consultar, RepositorioAuditoria auditoria) {
        this.registrar = registrar;
        this.cambiarEstado = cambiarEstado;
        this.consultar = consultar;
        this.auditoria = auditoria;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RespuestaPaciente> registrar(@Valid @RequestBody SolicitudRegistrarPaciente solicitud) {
        return registrar.ejecutar(solicitud.aComando());
    }

    @PatchMapping("/{id}/estado")
    public Mono<RespuestaPaciente> cambiarEstado(@PathVariable UUID id,
                                                 @Valid @RequestBody SolicitudCambiarEstado solicitud) {
        return cambiarEstado.ejecutar(new ComandoCambiarEstado(id, solicitud.estado()));
    }

    @GetMapping("/{id}")
    public Mono<RespuestaPaciente> consultar(@PathVariable UUID id) {
        return consultar.ejecutar(id);
    }

    /** Trazabilidad clínica (RN-04): solo lectura, no existe operación de borrado. */
    @GetMapping("/{id}/auditoria")
    public Flux<RegistroAuditoria> auditoria(@PathVariable UUID id) {
        return consultar.ejecutar(id).thenMany(auditoria.buscarPorPaciente(id));
    }
}
