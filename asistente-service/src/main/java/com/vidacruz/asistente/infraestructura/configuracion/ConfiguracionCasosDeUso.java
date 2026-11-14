package com.vidacruz.asistente.infraestructura.configuracion;

import com.vidacruz.asistente.aplicacion.manejador.ManejadorConsultarAsistente;
import com.vidacruz.asistente.aplicacion.manejador.ManejadorConsultarEstadoUrgencias;
import com.vidacruz.asistente.aplicacion.manejador.ManejadorProcesarEventoTriaje;
import com.vidacruz.asistente.dominio.puerto.BuscadorProtocolos;
import com.vidacruz.asistente.dominio.puerto.GeneradorRespuestas;
import com.vidacruz.asistente.dominio.puerto.MetricasAsistente;
import com.vidacruz.asistente.dominio.puerto.VistaEstadoUrgencias;
import com.vidacruz.asistente.dominio.servicio.PoliticaAntiAlucinacion;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensamblado explícito: dominio y casos de uso sin anotaciones de Spring, conectados aquí a los adaptadores. */
@Configuration
@EnableConfigurationProperties({PropiedadesRag.class, PropiedadesSeguridad.class})
public class ConfiguracionCasosDeUso {

    @Bean
    public PoliticaAntiAlucinacion politicaAntiAlucinacion(PropiedadesRag rag) {
        return new PoliticaAntiAlucinacion(rag.umbralSimilitud());
    }

    @Bean
    public ManejadorConsultarAsistente manejadorConsultarAsistente(BuscadorProtocolos buscador,
                                                                   GeneradorRespuestas generador,
                                                                   PoliticaAntiAlucinacion politica,
                                                                   MetricasAsistente metricas,
                                                                   PropiedadesRag rag) {
        return new ManejadorConsultarAsistente(buscador, generador, politica, metricas, rag.fragmentosARecuperar());
    }

    @Bean
    public ManejadorConsultarEstadoUrgencias manejadorConsultarEstadoUrgencias(VistaEstadoUrgencias vista) {
        return new ManejadorConsultarEstadoUrgencias(vista);
    }

    @Bean
    public ManejadorProcesarEventoTriaje manejadorProcesarEventoTriaje(VistaEstadoUrgencias vista) {
        return new ManejadorProcesarEventoTriaje(vista);
    }
}
