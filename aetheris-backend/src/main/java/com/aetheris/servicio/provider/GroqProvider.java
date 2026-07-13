package com.aetheris.servicio.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Proveedor principal: Groq (API compatible con OpenAI).
 * Usa llama-3.3-70b-versatile por defecto.
 * Variable de entorno requerida en Railway: GROQ_API_KEY
 *
 * Implementa streaming real (token a token) en chatStream para que
 * Railway/nginx no bufferice la respuesta SSE esperando el cuerpo completo.
 */
@Component
public class GroqProvider implements AiProvider {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Override
    public String getName() { return "Groq"; }

    @Override
    public boolean isAvailable() { return apiKey != null && !apiKey.isBlank(); }

    // ── Llamada no-streaming (usada en fallback y loops de tool-calling) ──

    @Override
    public JsonNode chat(ArrayNode messages, ArrayNode tools, ObjectMapper mapper) {
        if (!isAvailable()) {
            throw new RuntimeException("GROQ_API_KEY no está configurada");
        }
        try {
            ObjectNode body = buildBody(messages, tools, mapper, false);
            HttpRequest request = buildRequest(mapper.writeValueAsString(body));

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String msg = json.path("error").path("message").asText(response.body());
                throw new RuntimeException("Groq error (" + response.statusCode() + "): " + msg);
            }
            return json;

        } catch (IOException e) {
            throw new RuntimeException("Error de red al llamar a Groq: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Llamada a Groq interrumpida", e);
        }
    }

    // ── Llamada en streaming real: emite tokens vía onToken ───────────────

    /**
     * Llama a Groq con stream=true y procesa la respuesta línea a línea (SSE).
     * - Para fragmentos de texto: llama a {@code onToken} con cada chunk y lo acumula.
     * - Para tool_calls: los acumula entre chunks y los incluye en la respuesta sintética.
     * Al finalizar devuelve un JsonNode con la misma forma que {@link #chat}
     * (choices[0].message con content y/o tool_calls).
     */
    @Override
    public JsonNode chatStream(ArrayNode messages, ArrayNode tools, ObjectMapper mapper,
                                Consumer<String> onToken) {
        if (!isAvailable()) {
            throw new RuntimeException("GROQ_API_KEY no está configurada");
        }
        try {
            ObjectNode body = buildBody(messages, tools, mapper, true);
            HttpRequest request = buildRequest(mapper.writeValueAsString(body));

            HttpResponse<Stream<String>> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() >= 400) {
                // En modo streaming no podemos leer el body fácilmente; caemos al chat normal
                throw new RuntimeException("Groq streaming HTTP " + response.statusCode()
                        + " — intenta con chat() en su lugar");
            }

            StringBuilder           fullContent  = new StringBuilder();
            Map<Integer, ToolCallBuilder> tcMap   = new LinkedHashMap<>();

            try (Stream<String> lines = response.body()) {
                for (String line : (Iterable<String>) lines::iterator) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    JsonNode chunk;
                    try {
                        chunk = mapper.readTree(data);
                    } catch (Exception ignored) {
                        continue;
                    }

                    JsonNode delta = chunk.path("choices").path(0).path("delta");

                    // — Fragmento de texto normal —
                    String content = delta.path("content").asText(null);
                    if (content != null && !content.isEmpty()) {
                        fullContent.append(content);
                        onToken.accept(content);
                    }

                    // — Fragmento de tool_call (puede llegar en varios chunks) —
                    JsonNode tcArray = delta.path("tool_calls");
                    if (tcArray.isArray()) {
                        for (JsonNode tc : tcArray) {
                            int idx = tc.path("index").asInt(0);
                            ToolCallBuilder builder =
                                    tcMap.computeIfAbsent(idx, i -> new ToolCallBuilder());
                            if (tc.has("id"))   builder.id   = tc.path("id").asText();
                            if (tc.has("type")) builder.type = tc.path("type").asText("function");
                            JsonNode fn = tc.path("function");
                            if (!fn.isMissingNode()) {
                                if (fn.has("name"))      builder.name = fn.path("name").asText();
                                if (fn.has("arguments")) builder.arguments.append(
                                        fn.path("arguments").asText());
                            }
                        }
                    }
                }
            }

            return buildSyntheticResponse(mapper, fullContent.toString(), tcMap);

        } catch (IOException e) {
            throw new RuntimeException("Error de red al llamar a Groq (stream): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Streaming a Groq interrumpido", e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private ObjectNode buildBody(ArrayNode messages, ArrayNode tools,
                                  ObjectMapper mapper, boolean stream) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.set("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
        }
        body.put("temperature", 0.1);
        body.put("max_tokens", 4096);
        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    private HttpRequest buildRequest(String bodyJson) {
        return HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();
    }

    /**
     * Construye un JsonNode con la misma forma que la respuesta no-streaming de Groq:
     * { choices: [{ message: { role, content, tool_calls? } }] }
     */
    private JsonNode buildSyntheticResponse(ObjectMapper mapper, String content,
                                             Map<Integer, ToolCallBuilder> tcMap) {
        ObjectNode root    = mapper.createObjectNode();
        ArrayNode  choices = mapper.createArrayNode();
        ObjectNode choice  = mapper.createObjectNode();
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");

        if (!tcMap.isEmpty()) {
            // Hubo tool calls — content es null (estándar OpenAI)
            message.putNull("content");
            ArrayNode toolCalls = mapper.createArrayNode();
            for (ToolCallBuilder b : tcMap.values()) {
                ObjectNode tc = mapper.createObjectNode();
                tc.put("id",   b.id   != null ? b.id   : "call_" + b.name);
                tc.put("type", b.type != null ? b.type : "function");
                ObjectNode fn = mapper.createObjectNode();
                fn.put("name",      b.name != null ? b.name : "");
                fn.put("arguments", b.arguments.toString());
                tc.set("function", fn);
                toolCalls.add(tc);
            }
            message.set("tool_calls", toolCalls);
        } else {
            message.put("content", content);
        }

        choice.set("message", message);
        choices.add(choice);
        root.set("choices", choices);
        return root;
    }

    /** Acumulador de tool_call parciales durante el streaming. */
    private static class ToolCallBuilder {
        String        id;
        String        type;
        String        name;
        StringBuilder arguments = new StringBuilder();
    }
}
