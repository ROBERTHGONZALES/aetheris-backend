package com.aetheris.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP para la API de Groq (compatible con OpenAI).
 * Reemplaza la integración anterior con Google Gemini.
 * Endpoint: https://api.groq.com/openai/v1/chat/completions
 */
@Component
public class GeminiClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    /**
     * Envía una lista de mensajes (formato OpenAI) con herramientas a Groq
     * y devuelve el JSON de respuesta completo.
     */
    public JsonNode chat(ArrayNode messages, ArrayNode tools, ObjectMapper mapper) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GROQ_API_KEY no está configurada en el servidor. ARIA no puede responder.");
        }

        try {
            com.fasterxml.jackson.databind.node.ObjectNode body =
                    mapper.createObjectNode();
            body.put("model", model);
            body.set("messages", messages);
            body.set("tools", tools);
            body.put("temperature", 0.1);
            body.put("max_tokens", 4096);

            String requestJson = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String message = json.path("error").path("message").asText(response.body());
                throw new RuntimeException(
                        "Groq API error (" + response.statusCode() + "): " + message);
            }

            return json;

        } catch (IOException e) {
            throw new RuntimeException("Error al llamar a Groq: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Llamada a Groq interrumpida", e);
        }
    }
}
