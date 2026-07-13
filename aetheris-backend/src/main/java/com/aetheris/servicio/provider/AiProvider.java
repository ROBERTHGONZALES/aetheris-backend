package com.aetheris.servicio.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.function.Consumer;

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
     * Variante en streaming: invoca {@code onToken} con cada fragmento de texto
     * a medida que el proveedor lo va generando, y devuelve al final la misma
     * respuesta agregada que {@link #chat}.
     *
     * La implementación por defecto simplemente delega a {@link #chat} y no
     * llama a {@code onToken} — apropiado para proveedores rápidos (Groq,
     * Gemini, OpenRouter) donde el streaming real no aporta demasiado.
     * Los proveedores que sí se benefician de streaming real (ej. Ollama,
     * que corre en hardware local mucho más lento) deben sobreescribir este
     * método.
     */
    default JsonNode chatStream(ArrayNode messages, ArrayNode tools, ObjectMapper mapper,
                                 Consumer<String> onToken) {
        return chat(messages, tools, mapper);
    }
}
