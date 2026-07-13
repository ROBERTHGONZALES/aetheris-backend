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
 * Proveedor de respaldo local: Ollama (API compatible con OpenAI).
 *
 * Corre en la máquina del desarrollador y se expone al backend de Railway
 * mediante un túnel de Cloudflare (cloudflared) o ngrok.
 *
 * Configuración en Railway:
 *   OLLAMA_BASE_URL  → URL pública del túnel, ej: https://xyz.trycloudflare.com
 *   OLLAMA_MODEL     → modelo a usar (default: llama3.2:3b)
 *
 * Si OLLAMA_BASE_URL no está configurada, el proveedor se deshabilita
 * automáticamente y el orquestador lo salta sin error.
 *
 * Cómo activar en local:
 *   1. ollama serve                                    (inicia Ollama en :11434)
 *   2. ollama pull llama3.2:3b                         (descarga el modelo)
 *   3. cloudflared tunnel --url http://localhost:11434  (genera URL pública)
 *   4. Poner esa URL en OLLAMA_BASE_URL en Railway
 *   5. Agregar "ollama" al final de ARIA_PROVIDER_ORDER en Railway
 */
@Component
public class OllamaProvider implements AiProvider {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${ollama.base.url:}")
    private String baseUrl;

    @Value("${ollama.model:llama3.2:3b}")
    private String model;

    @Override
    public String getName() { return "Ollama"; }

    @Override
    public boolean isAvailable() { return baseUrl != null && !baseUrl.isBlank(); }

    @Override
    public JsonNode chat(ArrayNode messages, ArrayNode tools, ObjectMapper mapper) {
        if (!isAvailable()) {
            throw new RuntimeException("OLLAMA_BASE_URL no está configurada");
        }

        // Normalizar URL base (quitar slash final si existe)
        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.set("messages", messages);
            body.put("temperature", 0.1);
            body.put("stream", false);

            // Ollama soporta tool calling solo en modelos recientes (llama3.1+, llama3.2+).
            // Solo incluir tools si hay herramientas disponibles.
            if (tools != null && !tools.isEmpty()) {
                body.set("tools", tools);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBase + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(120)) // Más tiempo — CPU puede ser lento
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer ollama") // Ollama ignora el token
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String msg = json.path("error").path("message").asText(response.body());
                throw new RuntimeException("Ollama error (" + response.statusCode() + "): " + msg);
            }

            return json;

        } catch (IOException e) {
            throw new RuntimeException("Error de red al llamar a Ollama (¿el túnel está activo?): "
                    + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Llamada a Ollama interrumpida", e);
        }
    }
}
