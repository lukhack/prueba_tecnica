package com.vidacruz.asistente.infraestructura.configuracion;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ConfiguracionKafkaConsumidor {
    @Bean
    public KafkaReceiver<String, String> kafkaReceiver(KafkaProperties propiedades, KafkaConnectionDetails conexion,
                                                       @org.springframework.beans.factory.annotation.Value("${asistente.eventos.topic:triaje.eventos}") String topic) {
        Map<String, Object> config = new HashMap<>(propiedades.buildConsumerProperties());
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, conexion.getConsumerBootstrapServers());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        ReceiverOptions<String, String> opciones = ReceiverOptions.<String, String>create(config)
                .subscription(List.of(topic));
        return KafkaReceiver.create(opciones);
    }
}
