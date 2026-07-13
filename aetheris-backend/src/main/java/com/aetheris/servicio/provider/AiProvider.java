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
}
