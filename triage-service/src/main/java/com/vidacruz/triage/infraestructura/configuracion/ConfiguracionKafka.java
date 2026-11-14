package com.vidacruz.triage.infraestructura.configuracion;

import com.vidacruz.triage.infraestructura.salida.mensajeria.PropiedadesEventos;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(PropiedadesEventos.class)
public class ConfiguracionKafka {
    @Bean
    public KafkaSender<String, String> kafkaSender(KafkaProperties propiedades, KafkaConnectionDetails conexion) {
        Map<String, Object> config = new HashMap<>(propiedades.buildProducerProperties());
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, conexion.getProducerBootstrapServers());
        SenderOptions<String, String> opciones = SenderOptions.<String, String>create(config).maxInFlight(256);
        return KafkaSender.create(opciones);
    }

    @Bean
    public NewTopic topicTriajeEventos(PropiedadesEventos eventos) {
        return TopicBuilder.name(eventos.topic()).partitions(eventos.particiones()).replicas(1).build();
    }
}
