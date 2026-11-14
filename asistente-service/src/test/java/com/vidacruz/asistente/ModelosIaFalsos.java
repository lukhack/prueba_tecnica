package com.vidacruz.asistente;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@TestConfiguration
public class ModelosIaFalsos {
    @Bean
    public EmbeddingModel embeddingModelFalso() {
        return new EmbeddingModelFalso();
    }

    @Bean
    public ChatModel chatModelFalso() {
        return new ChatModelFalso();
    }

    public static class EmbeddingModelFalso implements EmbeddingModel {
        static final int DIMENSIONES = 512;
        static final Set<String> PALABRAS_VACIAS = Set.of("para", "ante", "cual", "cuales", "cuando", "como",
                "unos", "unas", "este", "esta", "tras", "entre", "sobre", "hacia", "desde", "hasta", "donde");

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            for (int i = 0; i < request.getInstructions().size(); i++) {
                embeddings.add(new Embedding(vector(request.getInstructions().get(i)), i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return vector(document.getText());
        }

        static float[] vector(String texto) {
            float[] v = new float[DIMENSIONES];
            String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT);
            for (String token : sinTildes.split("[^a-z]+")) {
                if (token.length() >= 4 && !PALABRAS_VACIAS.contains(token)) {
                    v[Math.floorMod(token.hashCode(), DIMENSIONES)] += 1f;
                }
            }
            double norma = 0;
            for (float x : v) norma += x * x;
            norma = Math.sqrt(norma);
            if (norma > 0) {
                for (int i = 0; i < v.length; i++) v[i] /= (float) norma;
            }
            return v;
        }
    }

    public static class ChatModelFalso implements ChatModel {
        public enum Modo { NORMAL, CAIDO, ALUCINA }

        public static final AtomicReference<Modo> MODO = new AtomicReference<>(Modo.NORMAL);

        @Override
        public ChatResponse call(Prompt prompt) {
            return stream(prompt).blockLast();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            if (MODO.get() == Modo.CAIDO) {
                return Flux.error(new IllegalStateException("Proveedor de IA caído (simulado)"));
            }
            String pregunta = extraerPregunta(prompt).toLowerCase(Locale.ROOT);
            String json;
            if (MODO.get() == Modo.ALUCINA) {
                json = """
                        {"respuesta": "Metformina como primera línea.", "fuentes": [{"protocolo": "PROTO-999", "seccion": "Diabetes"}], "sinProtocolo": false}""";
            } else if (pregunta.contains("hipertensiva")) {
                json = """
                        ```json
                        {"respuesta": "Reconfirmar la presión tras 5 minutos de reposo, no reducirla más de un 25% en la primera hora y vigilar signos de encefalopatía.",
                         "fuentes": [{"protocolo": "PROTO-001", "seccion": "Medidas iniciales"}], "sinProtocolo": false}
                        ```""";
            } else if (pregunta.contains("sepsis") || pregunta.contains("antibi")) {
                json = """
                        {"respuesta": "El antibiótico de amplio espectro se administra en la primera hora, tras tomar lactato y hemocultivos.",
                         "fuentes": [{"protocolo": "PROTO-002", "seccion": "Acciones iniciales"}], "sinProtocolo": false}""";
            } else {
                json = """
                        {"respuesta": "", "fuentes": [], "sinProtocolo": true}""";
            }

            int mitad = json.length() / 2;
            return Flux.just(trozo(json.substring(0, mitad)), trozo(json.substring(mitad)));
        }

        private static ChatResponse trozo(String texto) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(texto))));
        }

        private static String extraerPregunta(Prompt prompt) {
            String usuario = prompt.getInstructions().stream()
                    .filter(m -> m.getMessageType() == MessageType.USER)
                    .map(Message::getText)
                    .collect(Collectors.joining());
            int marca = usuario.lastIndexOf("PREGUNTA DEL MÉDICO:");
            return marca >= 0 ? usuario.substring(marca) : usuario;
        }
    }
}
