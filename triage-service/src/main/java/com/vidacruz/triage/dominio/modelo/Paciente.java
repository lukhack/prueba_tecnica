package com.vidacruz.triage.dominio.modelo;

import com.vidacruz.triage.dominio.excepcion.ExcepcionTransicionInvalida;
import com.vidacruz.triage.dominio.excepcion.ExcepcionValorInvalido;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raíz del triaje. Concentra las reglas de negocio sobre el ciclo de vida del paciente:
 * flujo de estados (RF-02), control de SLA y escalamiento idempotente (RF-03, RN-03).
 *
 * <p>Es una clase de dominio pura: no depende de Spring, de la base de datos ni de Kafka.</p>
 */
public class Paciente {

    private final UUID id;
    private final String nombreCompleto;
    private final int edad;
    private final SignosVitales signosVitales;
    private final String quejaPrincipal;
    private final NivelTriaje nivel;
    private final Instant fechaRegistro;

    private EstadoPaciente estado;
    private boolean escalado;
    private Instant fechaUltimoCambioEstado;
    private Instant fechaEntradaConsulta;
    private Instant fechaAlta;
    private Instant fechaEscalamiento;

    private Paciente(UUID id, String nombreCompleto, int edad, SignosVitales signosVitales, String quejaPrincipal,
                     NivelTriaje nivel, EstadoPaciente estado, boolean escalado, Instant fechaRegistro,
                     Instant fechaUltimoCambioEstado, Instant fechaEntradaConsulta, Instant fechaAlta,
                     Instant fechaEscalamiento) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.edad = edad;
        this.signosVitales = signosVitales;
        this.quejaPrincipal = quejaPrincipal;
        this.nivel = nivel;
        this.estado = estado;
        this.escalado = escalado;
        this.fechaRegistro = fechaRegistro;
        this.fechaUltimoCambioEstado = fechaUltimoCambioEstado;
        this.fechaEntradaConsulta = fechaEntradaConsulta;
        this.fechaAlta = fechaAlta;
        this.fechaEscalamiento = fechaEscalamiento;
    }

    /** Crea un paciente recién llegado a recepción, ya clasificado, en estado REGISTRADO (RF-01). */
    public static Paciente registrar(String nombreCompleto, int edad, SignosVitales signosVitales,
                                     String quejaPrincipal, NivelTriaje nivel, Instant ahora) {
        String nombre = nombreCompleto == null ? "" : nombreCompleto.trim();
        if (nombre.length() < 3 || nombre.length() > 100) {
            throw new ExcepcionValorInvalido("nombreCompleto", "El nombre completo debe tener entre 3 y 100 caracteres");
        }
        if (edad < 0 || edad > 120) {
            throw new ExcepcionValorInvalido("edad", "La edad debe estar entre 0 y 120 años (recibido: " + edad + ")");
        }
        String queja = quejaPrincipal == null ? "" : quejaPrincipal.trim();
        if (queja.isBlank()) {
            throw new ExcepcionValorInvalido("quejaPrincipal", "La queja principal es obligatoria");
        }
        Objects.requireNonNull(signosVitales, "signosVitales");
        Objects.requireNonNull(nivel, "nivel");
        return new Paciente(UUID.randomUUID(), nombre, edad, signosVitales, queja, nivel,
                EstadoPaciente.REGISTRADO, false, ahora, ahora, null, null, null);
    }

    /** Reconstruye el agregado desde la persistencia, sin volver a validar ni clasificar. */
    public static Paciente reconstruir(UUID id, String nombreCompleto, int edad, SignosVitales signosVitales,
                                       String quejaPrincipal, NivelTriaje nivel, EstadoPaciente estado, boolean escalado,
                                       Instant fechaRegistro, Instant fechaUltimoCambioEstado, Instant fechaEntradaConsulta,
                                       Instant fechaAlta, Instant fechaEscalamiento) {
        return new Paciente(id, nombreCompleto, edad, signosVitales, quejaPrincipal, nivel, estado, escalado,
                fechaRegistro, fechaUltimoCambioEstado, fechaEntradaConsulta, fechaAlta, fechaEscalamiento);
    }

    // ----- Comportamiento de negocio -----

    /**
     * Aplica una transición del flujo clínico (RF-02).
     *
     * @return el estado anterior, necesario para la auditoría
     * @throws ExcepcionTransicionInvalida si la transición no está permitida
     */
    public EstadoPaciente cambiarEstado(EstadoPaciente nuevoEstado, Instant ahora) {
        Objects.requireNonNull(nuevoEstado, "nuevoEstado");
        if (!estado.puedeTransicionarA(nuevoEstado)) {
            throw new ExcepcionTransicionInvalida(estado, nuevoEstado);
        }
        EstadoPaciente anterior = estado;
        estado = nuevoEstado;
        fechaUltimoCambioEstado = ahora;
        if (nuevoEstado == EstadoPaciente.EN_CONSULTA && fechaEntradaConsulta == null) {
            fechaEntradaConsulta = ahora;
        }
        if (nuevoEstado == EstadoPaciente.ALTA) {
            fechaAlta = ahora;
        }
        return anterior;
    }

    /** RN-02: el SLA se mide desde el registro hasta la entrada a consulta. */
    public boolean haSuperadoSla(Duration sla, Instant ahora) {
        return puedeSerEscalado() && !ahora.isBefore(fechaRegistro.plus(sla));
    }

    /** RN-03: solo se escala una vez y solo mientras el paciente sigue esperando (REGISTRADO). */
    public boolean puedeSerEscalado() {
        return estado == EstadoPaciente.REGISTRADO && !escalado;
    }

    /**
     * Marca el paciente como escalado (flag persistente, RF-03).
     *
     * @return {@code true} si el escalamiento se aplicó; {@code false} si ya estaba escalado
     *         o ya no está esperando (idempotencia, RN-03)
     */
    public boolean escalar(Instant ahora) {
        if (!puedeSerEscalado()) {
            return false;
        }
        escalado = true;
        fechaEscalamiento = ahora;
        return true;
    }

    public Duration tiempoEnEstadoActual(Instant ahora) {
        return Duration.between(fechaUltimoCambioEstado, ahora);
    }

    // ----- Accesores -----

    public UUID id() { return id; }
    public String nombreCompleto() { return nombreCompleto; }
    public int edad() { return edad; }
    public SignosVitales signosVitales() { return signosVitales; }
    public String quejaPrincipal() { return quejaPrincipal; }
    public NivelTriaje nivel() { return nivel; }
    public EstadoPaciente estado() { return estado; }
    public boolean escalado() { return escalado; }
    public Instant fechaRegistro() { return fechaRegistro; }
    public Instant fechaUltimoCambioEstado() { return fechaUltimoCambioEstado; }
    public Instant fechaEntradaConsulta() { return fechaEntradaConsulta; }
    public Instant fechaAlta() { return fechaAlta; }
    public Instant fechaEscalamiento() { return fechaEscalamiento; }
}
