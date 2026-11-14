package com.vidacruz.triage.infraestructura.salida.persistencia;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;
import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.modelo.Paciente;
import com.vidacruz.triage.dominio.modelo.SignosVitales;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Fila de la tabla {@code pacientes}. Es un detalle de infraestructura: el dominio nunca la ve.
 * La conversión hacia/desde {@link Paciente} vive aquí mismo para mantener el mapeo en un solo lugar.
 */
@Table("pacientes")
public class PacienteEntidad {

    @Id
    private UUID id;
    @Column("nombre_completo")
    private String nombreCompleto;
    private int edad;
    @Column("frecuencia_cardiaca")
    private int frecuenciaCardiaca;
    @Column("presion_sistolica")
    private int presionSistolica;
    @Column("presion_diastolica")
    private int presionDiastolica;
    private int spo2;
    private BigDecimal temperatura;
    private int dolor;
    private int gcs;
    @Column("queja_principal")
    private String quejaPrincipal;
    private String nivel;
    private String estado;
    private boolean escalado;
    @Column("fecha_registro")
    private OffsetDateTime fechaRegistro;
    @Column("fecha_ultimo_cambio_estado")
    private OffsetDateTime fechaUltimoCambioEstado;
    @Column("fecha_entrada_consulta")
    private OffsetDateTime fechaEntradaConsulta;
    @Column("fecha_alta")
    private OffsetDateTime fechaAlta;
    @Column("fecha_escalamiento")
    private OffsetDateTime fechaEscalamiento;
    /** Contador de cambios; lo incrementan las sentencias UPDATE explícitas del adaptador. */
    private Long version;

    public static PacienteEntidad desde(Paciente p) {
        PacienteEntidad e = new PacienteEntidad();
        e.id = p.id();
        e.nombreCompleto = p.nombreCompleto();
        e.edad = p.edad();
        SignosVitales s = p.signosVitales();
        e.frecuenciaCardiaca = s.frecuenciaCardiaca();
        e.presionSistolica = s.presionSistolica();
        e.presionDiastolica = s.presionDiastolica();
        e.spo2 = s.spo2();
        e.temperatura = s.temperatura();
        e.dolor = s.dolor();
        e.gcs = s.gcs();
        e.quejaPrincipal = p.quejaPrincipal();
        e.nivel = p.nivel().name();
        e.estado = p.estado().name();
        e.escalado = p.escalado();
        e.fechaRegistro = aFecha(p.fechaRegistro());
        e.fechaUltimoCambioEstado = aFecha(p.fechaUltimoCambioEstado());
        e.fechaEntradaConsulta = aFecha(p.fechaEntradaConsulta());
        e.fechaAlta = aFecha(p.fechaAlta());
        e.fechaEscalamiento = aFecha(p.fechaEscalamiento());
        return e;
    }

    public Paciente aDominio() {
        SignosVitales signos = new SignosVitales(frecuenciaCardiaca, presionSistolica, presionDiastolica,
                spo2, temperatura, dolor, gcs);
        return Paciente.reconstruir(id, nombreCompleto, edad, signos, quejaPrincipal,
                NivelTriaje.valueOf(nivel), EstadoPaciente.valueOf(estado), escalado,
                aInstante(fechaRegistro), aInstante(fechaUltimoCambioEstado), aInstante(fechaEntradaConsulta),
                aInstante(fechaAlta), aInstante(fechaEscalamiento));
    }

    static OffsetDateTime aFecha(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }

    static Instant aInstante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
