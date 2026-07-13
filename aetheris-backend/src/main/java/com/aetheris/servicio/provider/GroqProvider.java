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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class GroqProvider implements AiProvider {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Override public String getName() { return "Groq"; }

    @Override
    public boolean isAvailable() { return apiKey != null && !apiKey.isBlank(); }

    // ── Llamada no-streaming ───────────────────────────────────────────────

    @Override
    public JsonNode chat(ArrayNode messages, ArrayNode tools, ObjectMapper mapper) {
        if (!isAvailable()) throw new RuntimeException("GROQ_API_KEY no está configurada");
        try {
            String bodyJson = mapper.writeValueAsString(buildBody(messages, tools, mapper, false));
            HttpResponse<String> response =
                    httpClient.send(buildRequest(bodyJson), HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Groq error (" + response.statusCode() + "): "
                        + json.path("error").path("message").asText(response.body()));
            }
            return json;
        } catch (IOException e) {
            throw new RuntimeException("Error de red con Groq: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Llamada a Groq interrumpida", e);
        }
    }

    // ── Streaming real: un onToken por cada fragmento de texto ────────────

    @Override
    public JsonNode chatStream(ArrayNode messages, ArrayNode tools, ObjectMapper mapper,
                                Consumer<String> onToken) {
        if (!isAvailable()) throw new RuntimeException("GROQ_API_KEY no está configurada");
        try {
            String bodyJson = mapper.writeValueAsString(buildBody(messages, tools, mapper, true));
            HttpResponse<java.io.InputStream> response =
                    httpClient.send(buildRequest(bodyJson), HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Groq streaming HTTP " + response.statusCode());
            }

            StringBuilder fullContent = new StringBuilder();
            Map<Integer, ToolCallBuilder> tcMap = new LinkedHashMap<>();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body(), java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;
                if (data.isEmpty()) continue;

                JsonNode chunk;
                try { chunk = mapper.readTree(data); }
                catch (Exception ignored) { continue; }

                JsonNode delta = chunk.path("choices").path(0).path("delta");

                // Fragmento de texto
                String content = delta.path("content").asText(null);
                if (content != null && !content.isEmpty()) {
                    fullContent.append(content);
                    onToken.accept(content);
                }

                // Fragmento de tool_call (acumular entre chunks)
                JsonNode tcArray = delta.path("tool_calls");
                if (tcArray.isArray()) {
                    for (JsonNode tc : tcArray) {
                        int idx = tc.path("index").asInt(0);
                        ToolCallBuilder b = tcMap.computeIfAbsent(idx, i -> new ToolCallBuilder());
                        if (tc.has("id"))   b.id   = tc.path("id").asText();
                        if (tc.has("type")) b.type = tc.path("type").asText("function");
                        JsonNode fn = tc.path("function");
                        if (!fn.isMissingNode()) {
                            if (fn.has("name"))      b.name = fn.path("name").asText();
                            if (fn.has("arguments")) b.arguments.append(fn.path("arguments").asText());
                        }
                    }
                }
            }
            reader.close();

            return buildResponse(mapper, fullContent.toString(), tcMap);

        } catch (IOException e) {
            throw new RuntimeException("Error de red con Groq (stream): " + e.getMessage(), e);
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
        if (tools != null && !tools.isEmpty()) body.set("tools", tools);
        body.put("temperature", 0.1);
        body.put("max_tokens", 4096);
        if (stream) body.put("stream", true);
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

    private JsonNode buildResponse(ObjectMapper mapper, String content,
                                    Map<Integer, ToolCallBuilder> tcMap) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode choices = mapper.createArrayNode();
        ObjectNode choice = mapper.createObjectNode();
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");
        if (!tcMap.isEmpty()) {
            message.putNull("content");
            ArrayNode tcs = mapper.createArrayNode();
            for (ToolCallBuilder b : tcMap.values()) {
                ObjectNode tc = mapper.createObjectNode();
                tc.put("id",   b.id   != null ? b.id   : "call_" + b.name);
                tc.put("type", b.type != null ? b.type : "function");
                ObjectNode fn = mapper.createObjectNode();
                fn.put("name",      b.name != null ? b.name : "");
                fn.put("arguments", b.arguments.toString());
                tc.set("function", fn);
                tcs.add(tc);
            }
            message.set("tool_calls", tcs);
        } else {
            message.put("content", content);
        }
        choice.set("message", message);
        choices.add(choice);
        root.set("choices", choices);
        return root;
    }

    private static class ToolCallBuilder {
        String id, type, name;
        StringBuilder arguments = new StringBuilder();
    }
}
