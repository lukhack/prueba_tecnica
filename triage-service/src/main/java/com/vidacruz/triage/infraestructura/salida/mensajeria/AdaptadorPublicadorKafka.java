package com.vidacruz.triage.infraestructura.salida.mensajeria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.triage.dominio.modelo.EventoTriaje;
import com.vidacruz.triage.dominio.puerto.PublicadorEventos;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Adaptador Kafka del puerto {@link PublicadorEventos}: publica eventos JSON en {@code triaje.eventos}.
 * La clave del mensaje es el id del paciente, lo que garantiza orden por paciente dentro de una partición.
 */
@Component
public class AdaptadorPublicadorKafka implements PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(AdaptadorPublicadorKafka.class);

    private final KafkaSender<String, String> sender;
    private final ObjectMapper objectMapper;
    private final PropiedadesEventos propiedades;

    public AdaptadorPublicadorKafka(KafkaSender<String, String> sender, ObjectMapper objectMapper,
                                    PropiedadesEventos propiedades) {
        this.sender = sender;
        this.objectMapper = objectMapper;
        this.propiedades = propiedades;
    }

    @Override
    public Mono<Void> publicar(EventoTriaje evento) {
        return Mono.fromCallable(() -> serializar(evento))
                .map(json -> SenderRecord.create(
                        new ProducerRecord<>(propiedades.topic(), evento.pacienteId().toString(), json),
                        evento.eventoId()))
                .flatMapMany(registro -> sender.send(Mono.just(registro)))
                .next()
                .flatMap(resultado -> resultado.exception() != null
                        ? Mono.error(resultado.exception())
                        : Mono.empty())
                .doOnSuccess(v -> log.debug("Evento {} publicado para paciente {}", evento.tipo(), evento.pacienteId()))
                .then();
    }

    private String serializar(EventoTriaje evento) throws JsonProcessingException {
        return objectMapper.writeValueAsString(evento);
    }
}
