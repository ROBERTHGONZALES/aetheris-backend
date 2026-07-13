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

/**
 * Proveedor de respaldo #2: OpenRouter.
 * API 100% compatible con OpenAI — solo cambia el endpoint y el modelo.
 * Variable de entorno requerida en Railway: OPENROUTER_API_KEY
 * Clave gratuita en: https://openrouter.ai
 */
@Component
public class OpenRouterProvider implements AiProvider {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.model:meta-llama/llama-3.3-70b-instruct:free}")
    private String model;

    @Override
    public String getName() { return "OpenRouter"; }

    @Override
    public boolean isAvailable() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public JsonNode chat(ArrayNode messages, ArrayNode tools, ObjectMapper mapper) {
        if (!isAvailable()) {
            throw new RuntimeException("OPENROUTER_API_KEY no está configurada");
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.set("messages", messages);
            body.set("tools", tools);
            body.put("temperature", 0.1);
            body.put("max_tokens", 4096);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "https://aetheris.app")
                    .header("X-Title", "Aetheris")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String msg = json.path("error").path("message").asText(response.body());
                throw new RuntimeException("OpenRouter error (" + response.statusCode() + "): " + msg);
            }
            return json;

        } catch (IOException e) {
            throw new RuntimeException("Error de red al llamar a OpenRouter: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Llamada a OpenRouter interrumpida", e);
        }
    }
}
