package com.vidacruz.asistente.infraestructura.salida.redis;

import com.vidacruz.asistente.dominio.modelo.EstadoUrgencias;
import com.vidacruz.asistente.dominio.modelo.EventoTriajeRecibido;
import com.vidacruz.asistente.dominio.modelo.NivelTriaje;
import com.vidacruz.asistente.dominio.puerto.VistaEstadoUrgencias;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AdaptadorVistaUrgenciasRedis implements VistaEstadoUrgencias {
    static final String CLAVE_ACTIVOS = "urgencias:activos";
    static final String CLAVE_ACTUALIZADO = "urgencias:actualizado";
    static final String PREFIJO_PACIENTE = "urgencias:paciente:";
    static final Duration TTL_PACIENTE = Duration.ofDays(7);

    static final String SCRIPT_APLICAR = """
            local estadoActual = redis.call('HGET', KEYS[1], 'estado')
            if estadoActual == 'ALTA' then
              return 0
            end
            local nivel = redis.call('HGET', KEYS[1], 'nivel')
            if not nivel then nivel = ARGV[1] end
            local eraActivo = (estadoActual ~= false)
            local seraActivo = (ARGV[3] == '1')
            if estadoActual == ARGV[2] then
              return 0
            end
            redis.call('HSET', KEYS[1], 'estado', ARGV[2], 'nivel', nivel)
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
            if (not eraActivo) and seraActivo then
              redis.call('HINCRBY', KEYS[2], nivel, 1)
            elseif eraActivo and (not seraActivo) then
              redis.call('HINCRBY', KEYS[2], nivel, -1)
            end
            return 1
            """;

    private static final RedisScript<Long> APLICAR = new DefaultRedisScript<>(SCRIPT_APLICAR, Long.class);

    private final ReactiveStringRedisTemplate redis;

    public AdaptadorVistaUrgenciasRedis(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Boolean> aplicar(EventoTriajeRecibido evento) {
        String estado = evento.estado() == null ? "REGISTRADO" : evento.estado();
        return redis.execute(APLICAR,
                        List.of(PREFIJO_PACIENTE + evento.pacienteId(), CLAVE_ACTIVOS),
                        List.of(evento.nivel().name(), estado, evento.pacienteActivo() ? "1" : "0",
                                String.valueOf(TTL_PACIENTE.toSeconds())))
                .next()
                .map(resultado -> resultado != null && resultado == 1L)
                .flatMap(cambio -> cambio
                        ? redis.opsForValue().set(CLAVE_ACTUALIZADO, Instant.now().toString()).thenReturn(true)
                        : Mono.just(false));
    }

    @Override
    public Mono<EstadoUrgencias> consultar() {
        Mono<Map<NivelTriaje, Long>> conteos = redis.<String, String>opsForHash().entries(CLAVE_ACTIVOS)
                .collect(() -> new EnumMap<NivelTriaje, Long>(NivelTriaje.class),
                        (mapa, entrada) -> mapa.put(NivelTriaje.valueOf(entrada.getKey()), Long.parseLong(entrada.getValue())));
        Mono<Instant> actualizado = redis.opsForValue().get(CLAVE_ACTUALIZADO)
                .map(Instant::parse)
                .defaultIfEmpty(Instant.EPOCH);
        return Mono.zip(conteos, actualizado, EstadoUrgencias::new);
    }
}
