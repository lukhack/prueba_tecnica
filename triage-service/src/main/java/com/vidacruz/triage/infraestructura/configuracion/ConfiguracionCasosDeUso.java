package com.vidacruz.triage.infraestructura.configuracion;

import com.vidacruz.triage.aplicacion.manejador.ManejadorCambiarEstado;
import com.vidacruz.triage.aplicacion.manejador.ManejadorConsultarPaciente;
import com.vidacruz.triage.aplicacion.manejador.ManejadorEscalarPacientesVencidos;
import com.vidacruz.triage.aplicacion.manejador.ManejadorRegistrarPaciente;
import com.vidacruz.triage.aplicacion.manejador.ManejadorReporteOperacion;
import com.vidacruz.triage.dominio.puerto.MetricasTriaje;
import com.vidacruz.triage.dominio.puerto.NotificadorEquipoMedico;
import com.vidacruz.triage.dominio.puerto.PoliticaSla;
import com.vidacruz.triage.dominio.puerto.PublicadorEventos;
import com.vidacruz.triage.dominio.puerto.RepositorioAuditoria;
import com.vidacruz.triage.dominio.puerto.RepositorioPacientes;
import com.vidacruz.triage.dominio.puerto.RepositorioReportes;
import com.vidacruz.triage.dominio.servicio.ClasificadorTriaje;
import com.vidacruz.triage.infraestructura.salida.alertas.PropiedadesAlertas;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.time.Clock;

/**
 * Ensamblado explícito de la aplicación: el dominio y los casos de uso no llevan anotaciones de Spring;
 * aquí se conectan con los adaptadores (inyección de dependencias por constructor).
 */
@Configuration
@EnableConfigurationProperties({PropiedadesSla.class, PropiedadesEscalamiento.class,
        PropiedadesSeguridad.class, PropiedadesAlertas.class})
public class ConfiguracionCasosDeUso {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public ClasificadorTriaje clasificadorTriaje() {
        return new ClasificadorTriaje();
    }

    @Bean
    public ManejadorRegistrarPaciente manejadorRegistrarPaciente(ClasificadorTriaje clasificador,
                                                                 RepositorioPacientes pacientes,
                                                                 RepositorioAuditoria auditoria,
                                                                 PublicadorEventos publicador,
                                                                 PoliticaSla politicaSla,
                                                                 MetricasTriaje metricas,
                                                                 TransactionalOperator transacciones,
                                                                 Clock reloj) {
        return new ManejadorRegistrarPaciente(clasificador, pacientes, auditoria, publicador, politicaSla,
                metricas, transacciones, reloj);
    }

    @Bean
    public ManejadorCambiarEstado manejadorCambiarEstado(RepositorioPacientes pacientes,
                                                         RepositorioAuditoria auditoria,
                                                         PublicadorEventos publicador,
                                                         PoliticaSla politicaSla,
                                                         TransactionalOperator transacciones,
                                                         Clock reloj) {
        return new ManejadorCambiarEstado(pacientes, auditoria, publicador, politicaSla, transacciones, reloj);
    }

    @Bean
    public ManejadorConsultarPaciente manejadorConsultarPaciente(RepositorioPacientes pacientes,
                                                                 PoliticaSla politicaSla, Clock reloj) {
        return new ManejadorConsultarPaciente(pacientes, politicaSla, reloj);
    }

    @Bean
    public ManejadorEscalarPacientesVencidos manejadorEscalarPacientesVencidos(RepositorioPacientes pacientes,
                                                                               RepositorioAuditoria auditoria,
                                                                               PublicadorEventos publicador,
                                                                               NotificadorEquipoMedico notificador,
                                                                               PoliticaSla politicaSla,
                                                                               MetricasTriaje metricas,
                                                                               TransactionalOperator transacciones,
                                                                               Clock reloj) {
        return new ManejadorEscalarPacientesVencidos(pacientes, auditoria, publicador, notificador, politicaSla,
                metricas, transacciones, reloj);
    }

    @Bean
    public ManejadorReporteOperacion manejadorReporteOperacion(RepositorioReportes reportes) {
        return new ManejadorReporteOperacion(reportes);
    }
}
