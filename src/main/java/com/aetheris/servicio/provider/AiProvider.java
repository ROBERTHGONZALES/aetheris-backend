package com.aetheris.servicio.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Contrato común para todos los proveedores de IA de ARIA.
 * Cada implementación recibe mensajes en formato OpenAI y devuelve
 * una respuesta también en formato OpenAI (conversión interna si hace falta).
 */
public interface AiProvider {
    /** Nombre legible del proveedor (ej: "Groq", "Gemini", "OpenRouter"). */
    String getName();

    /** Indica si la API key está configurada y el proveedor puede usarse. */
    boolean isAvailable();

    /**
     * Envía los mensajes al proveedor y devuelve la respuesta en formato
     * OpenAI chat/completions (choices[0].message…).
     */
    JsonNode chat(ArrayNode messages, ArrayNode tools, ObjectMapper mapper);

    /**
     * Igual que chat(), pero invoca onDelta(texto) por cada fragmento de
     * contenido recibido del proveedor a medida que llega, permitiendo
     * streaming real hacia el cliente (SSE token por token).
     *
     * Implementación por defecto (fallback): no transmite en partes, llama
     * a chat() y entrega todo el texto de una sola vez al final. La usan
     * los proveedores que aún no implementan streaming real (ej. Gemini,
     * OpenRouter). Groq y Ollama la sobrescriben con streaming verdadero.
     */
    default JsonNode chatStream(ArrayNode messages, ArrayNode tools, ObjectMapper mapper,
                                 java.util.function.Consumer<String> onDelta) {
        JsonNode response = chat(messages, tools, mapper);
        String text = response.path("choices").path(0).path("message").path("content").asText("");
        if (!text.isBlank()) {
            onDelta.accept(text);
        }
        return response;
    }
}
