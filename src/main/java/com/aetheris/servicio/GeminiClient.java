package com.aetheris.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Cliente HTTP minimalista para la API REST de Gemini (Google AI Studio).
 * No usa un SDK: llama directamente a generativelanguage.googleapis.com,
 * ya que la app solo necesita generateContent con function calling.
 */
@Component
public class GeminiClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    /**
     * Envía un cuerpo de solicitud generateContent ya construido (contents,
     * systemInstruction, tools) y devuelve el JSON de respuesta de Gemini.
     */
    public JsonNode generateContent(ObjectNode requestBody, ObjectMapper mapper) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY no está configurada en el servidor. ARIA no puede responder.");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String message = json.path("error").path("message").asText(response.body());
                throw new RuntimeException("Gemini API error (" + response.statusCode() + "): " + message);
            }
            return json;
        } catch (IOException e) {
            throw new RuntimeException("Error al llamar a Gemini: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Llamada a Gemini interrumpida", e);
        }
    }
}
