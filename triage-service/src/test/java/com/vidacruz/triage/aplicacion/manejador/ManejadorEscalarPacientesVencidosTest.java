package com.vidacruz.triage.aplicacion.manejador;

import com.vidacruz.triage.dominio.modelo.EstadoPaciente;
import com.vidacruz.triage.dominio.modelo.EventoTriaje;
import com.vidacruz.triage.dominio.modelo.NivelTriaje;
import com.vidacruz.triage.dominio.modelo.Paciente;
import com.vidacruz.triage.dominio.modelo.RegistroAuditoria;
import com.vidacruz.triage.dominio.modelo.SignosVitales;
import com.vidacruz.triage.dominio.modelo.TipoEvento;
import com.vidacruz.triage.dominio.puerto.MetricasTriaje;
import com.vidacruz.triage.dominio.puerto.NotificadorEquipoMedico;
import com.vidacruz.triage.dominio.puerto.PoliticaSla;
import com.vidacruz.triage.dominio.puerto.PublicadorEventos;
import com.vidacruz.triage.dominio.puerto.RepositorioAuditoria;
import com.vidacruz.triage.dominio.puerto.RepositorioPacientes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF-03: caso de uso de escalamiento por SLA")
class ManejadorEscalarPacientesVencidosTest {
    private static final Instant AHORA = Instant.parse("2026-11-14T10:30:00Z");
    private static final PoliticaSla SLA_DE_LA_REGLA = nivel -> switch (nivel) {
        case CRITICO -> Duration.ofMinutes(5);
        case URGENTE -> Duration.ofMinutes(10);
        case URGENTE_2 -> Duration.ofMinutes(30);
        case NO_URGENTE -> Duration.ofMinutes(60);
        case NO_URGENTE_2 -> Duration.ofMinutes(120);
    };

    @Mock RepositorioPacientes pacientes;
    @Mock RepositorioAuditoria auditoria;
    @Mock PublicadorEventos publicador;
    @Mock NotificadorEquipoMedico notificador;
    @Mock MetricasTriaje metricas;
    @Mock TransactionalOperator transacciones;

    private ManejadorEscalarPacientesVencidos manejador;

    @BeforeEach
    void setUp() {
        manejador = new ManejadorEscalarPacientesVencidos(pacientes, auditoria, publicador, notificador,
                SLA_DE_LA_REGLA, metricas, transacciones, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void escalaAlPacienteQueSuperoSuSlaYPublicaUnSoloEvento() {
        Paciente vencido = pacienteRegistradoHace(NivelTriaje.CRITICO, Duration.ofMinutes(6));
        Paciente aTiempo = pacienteRegistradoHace(NivelTriaje.URGENTE, Duration.ofMinutes(6));
        when(pacientes.buscarRegistradosNoEscalados()).thenReturn(Flux.just(vencido, aTiempo));
        when(pacientes.marcarEscaladoSiAplica(vencido.id(), AHORA)).thenReturn(Mono.just(true));
        when(transacciones.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditoria.registrar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publicador.publicar(any())).thenReturn(Mono.empty());
        when(notificador.notificarEscalamiento(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(manejador.ejecutar()).expectNext(1L).verifyComplete();

        assertThat(vencido.escalado()).isTrue();
        assertThat(aTiempo.escalado()).isFalse();

        ArgumentCaptor<RegistroAuditoria> registro = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoria).registrar(registro.capture());
        assertThat(registro.getValue().tipoEvento()).isEqualTo(TipoEvento.PACIENTE_ESCALADO);
        assertThat(registro.getValue().pacienteId()).isEqualTo(vencido.id());

        ArgumentCaptor<EventoTriaje> evento = ArgumentCaptor.forClass(EventoTriaje.class);
        verify(publicador).publicar(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo("paciente_escalado");
        assertThat(evento.getValue().escalado()).isTrue();
        assertThat(evento.getValue().estado()).isEqualTo(EstadoPaciente.REGISTRADO);

        verify(notificador).notificarEscalamiento(eq(vencido), eq(Duration.ofMinutes(6)));
        verify(metricas).registrarEscalamiento(NivelTriaje.CRITICO);
        verify(pacientes, never()).marcarEscaladoSiAplica(eq(aTiempo.id()), any());
    }

    @Test
    void noDuplicaEventoNiAuditoriaSiOtraInstanciaYaLoEscalo() {
        Paciente vencido = pacienteRegistradoHace(NivelTriaje.CRITICO, Duration.ofMinutes(10));
        when(pacientes.buscarRegistradosNoEscalados()).thenReturn(Flux.just(vencido));

        when(pacientes.marcarEscaladoSiAplica(vencido.id(), AHORA)).thenReturn(Mono.just(false));
        when(transacciones.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(manejador.ejecutar()).expectNext(0L).verifyComplete();

        verify(auditoria, never()).registrar(any());
        verify(publicador, never()).publicar(any());
        verify(notificador, never()).notificarEscalamiento(any(), any());
    }

    @Test
    void siNoHayPacientesVencidosNoHaceNada() {
        when(pacientes.buscarRegistradosNoEscalados())
                .thenReturn(Flux.just(pacienteRegistradoHace(NivelTriaje.NO_URGENTE, Duration.ofMinutes(59))));

        StepVerifier.create(manejador.ejecutar()).expectNext(0L).verifyComplete();

        verify(pacientes, never()).marcarEscaladoSiAplica(any(), any());
    }

    @Test
    void unFalloEnLaAlertaNoImpideElEscalamiento() {
        Paciente vencido = pacienteRegistradoHace(NivelTriaje.URGENTE_2, Duration.ofMinutes(31));
        when(pacientes.buscarRegistradosNoEscalados()).thenReturn(Flux.just(vencido));
        when(pacientes.marcarEscaladoSiAplica(vencido.id(), AHORA)).thenReturn(Mono.just(true));
        when(transacciones.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditoria.registrar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publicador.publicar(any())).thenReturn(Mono.empty());
        when(notificador.notificarEscalamiento(any(), any())).thenReturn(Mono.error(new IllegalStateException("webhook caído")));

        StepVerifier.create(manejador.ejecutar()).expectNext(1L).verifyComplete();

        verify(publicador).publicar(any());
    }

    private static Paciente pacienteRegistradoHace(NivelTriaje nivel, Duration hace) {
        SignosVitales signos = new SignosVitales(72, 120, 80, 99, new BigDecimal("36.5"), 2, 15);
        return Paciente.registrar("Paciente Prueba", 40, signos, "Consulta", nivel, AHORA.minus(hace));
    }
}
