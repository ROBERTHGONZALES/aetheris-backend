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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Proveedor de respaldo #1: Google Gemini.
 * Internamente convierte los mensajes de formato OpenAI ↔ formato Gemini,
 * por lo que AriaService no necesita saber que habla con Gemini.
 *
 * Diferencias clave de formato:
 *   - systemInstruction es un campo raíz, no un mensaje
 *   - "tool" messages → functionResponse en un turno "user"
 *   - Tipos de parámetros en MAYÚSCULAS (STRING, OBJECT...)
 *   - Respuesta en candidates[0].content.parts en vez de choices[0].message
 *
 * Variable de entorno requerida en Railway: GEMINI_API_KEY
 * Clave gratuita en: https://aistudio.google.com
 */
@Component
public class GeminiProvider implements AiProvider {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    @Override
    public String getName() { return "Gemini"; }

    @Override
    public boolean isAvailable() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public JsonNode chat(ArrayNode messages, ArrayNode tools, ObjectMapper mapper) {
        if (!isAvailable()) {
            throw new RuntimeException("GEMINI_API_KEY no está configurada");
        }
        try {
            ObjectNode geminiBody = toGeminiRequest(messages, tools, mapper);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(geminiBody)))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String msg = json.path("error").path("message").asText(response.body());
                throw new RuntimeException("Gemini error (" + response.statusCode() + "): " + msg);
            }
            return toOpenAiResponse(json, mapper);

        } catch (IOException e) {
            throw new RuntimeException("Error de red al llamar a Gemini: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Llamada a Gemini interrumpida", e);
        }
    }

    // ── Conversión OpenAI → Gemini ─────────────────────────────────────────

    private ObjectNode toGeminiRequest(ArrayNode messages, ArrayNode tools, ObjectMapper mapper)
            throws IOException {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode contents = mapper.createArrayNode();

        // Mapa de toolCallId → funcName para resolver functionResponse
        Map<String, String> callIdToName = new HashMap<>();

        for (JsonNode msg : messages) {
            String role = msg.path("role").asText();

            switch (role) {
                case "system" -> {
                    // systemInstruction como campo raíz (no va en contents)
                    ObjectNode si = mapper.createObjectNode();
                    ArrayNode siParts = mapper.createArrayNode();
                    ObjectNode siText = mapper.createObjectNode();
                    siText.put("text", msg.path("content").asText());
                    siParts.add(siText);
                    si.set("parts", siParts);
                    body.set("systemInstruction", si);
                }
                case "user" -> {
                    ObjectNode c = mapper.createObjectNode();
                    c.put("role", "user");
                    ArrayNode parts = mapper.createArrayNode();
                    ObjectNode part = mapper.createObjectNode();
                    part.put("text", msg.path("content").asText(""));
                    parts.add(part);
                    c.set("parts", parts);
                    contents.add(c);
                }
                case "assistant" -> {
                    ObjectNode c = mapper.createObjectNode();
                    c.put("role", "model");
                    ArrayNode parts = mapper.createArrayNode();

                    String text = msg.path("content").asText("");
                    if (!text.isBlank()) {
                        ObjectNode tp = mapper.createObjectNode();
                        tp.put("text", text);
                        parts.add(tp);
                    }

                    JsonNode toolCalls = msg.path("tool_calls");
                    if (toolCalls.isArray()) {
                        for (JsonNode tc : toolCalls) {
                            String callId  = tc.path("id").asText();
                            String name    = tc.path("function").path("name").asText();
                            String argsStr = tc.path("function").path("arguments").asText("{}");
                            callIdToName.put(callId, name);

                            ObjectNode fcPart = mapper.createObjectNode();
                            ObjectNode fc     = mapper.createObjectNode();
                            fc.put("name", name);
                            fc.set("args", mapper.readTree(argsStr));
                            fcPart.set("functionCall", fc);
                            parts.add(fcPart);
                        }
                    }
                    c.set("parts", parts);
                    contents.add(c);
                }
                case "tool" -> {
                    // Gemini agrupa los resultados de herramientas como un turno "user"
                    String callId   = msg.path("tool_call_id").asText();
                    String funcName = callIdToName.getOrDefault(callId, "function");
                    String result   = msg.path("content").asText("{}");

                    ObjectNode c    = mapper.createObjectNode();
                    c.put("role", "user");
                    ArrayNode parts = mapper.createArrayNode();
                    ObjectNode frPart = mapper.createObjectNode();
                    ObjectNode fr     = mapper.createObjectNode();
                    fr.put("name", funcName);
                    ObjectNode resp = mapper.createObjectNode();
                    try {
                        resp.set("result", mapper.readTree(result));
                    } catch (Exception ex) {
                        resp.put("result", result);
                    }
                    fr.set("response", resp);
                    frPart.set("functionResponse", fr);
                    parts.add(frPart);
                    c.set("parts", parts);
                    contents.add(c);
                }
            }
        }

        body.set("contents", contents);

        // Convertir tools: formato OpenAI → Gemini functionDeclarations
        if (tools != null && !tools.isEmpty()) {
            ArrayNode decls = mapper.createArrayNode();
            for (JsonNode tool : tools) {
                JsonNode func = tool.path("function");
                ObjectNode decl = mapper.createObjectNode();
                decl.put("name", func.path("name").asText());
                decl.put("description", func.path("description").asText());
                decl.set("parameters", toGeminiSchema(func.path("parameters"), mapper));
                decls.add(decl);
            }
            ArrayNode toolsArr = mapper.createArrayNode();
            ObjectNode toolsObj = mapper.createObjectNode();
            toolsObj.set("functionDeclarations", decls);
            toolsArr.add(toolsObj);
            body.set("tools", toolsArr);
        }

        // Config de generación
        ObjectNode genCfg = mapper.createObjectNode();
        genCfg.put("temperature", 0.1);
        genCfg.put("maxOutputTokens", 4096);
        body.set("generationConfig", genCfg);

        return body;
    }

    /**
     * Convierte un JSON Schema de formato OpenAI (tipos en minúscula) a
     * formato Gemini (tipos en MAYÚSCULA: STRING, OBJECT, ARRAY, etc.).
     */
    private JsonNode toGeminiSchema(JsonNode schema, ObjectMapper mapper) {
        if (!schema.isObject()) return schema;
        ObjectNode result = mapper.createObjectNode();
        schema.fields().forEachRemaining(entry -> {
            String  key   = entry.getKey();
            JsonNode val  = entry.getValue();
            if ("type".equals(key) && val.isTextual()) {
                result.put("type", val.asText().toUpperCase());
            } else if ("properties".equals(key) && val.isObject()) {
                ObjectNode props = mapper.createObjectNode();
                val.fields().forEachRemaining(p ->
                        props.set(p.getKey(), toGeminiSchema(p.getValue(), mapper)));
                result.set("properties", props);
            } else {
                result.set(key, val);
            }
        });
        return result;
    }

    // ── Conversión Gemini → OpenAI ─────────────────────────────────────────

    private JsonNode toOpenAiResponse(JsonNode geminiResp, ObjectMapper mapper) {
        ObjectNode openAi = mapper.createObjectNode();
        ArrayNode  choices = mapper.createArrayNode();

        JsonNode parts = geminiResp
                .path("candidates").path(0)
                .path("content").path("parts");

        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");

        StringBuilder textBuf  = new StringBuilder();
        ArrayNode     toolCalls = mapper.createArrayNode();

        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    textBuf.append(part.path("text").asText());
                } else if (part.has("functionCall")) {
                    JsonNode fc  = part.path("functionCall");
                    ObjectNode tc = mapper.createObjectNode();
                    tc.put("id",   "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
                    tc.put("type", "function");
                    ObjectNode func = mapper.createObjectNode();
                    func.put("name", fc.path("name").asText());
                    try {
                        func.put("arguments", mapper.writeValueAsString(fc.path("args")));
                    } catch (Exception e) {
                        func.put("arguments", "{}");
                    }
                    tc.set("function", func);
                    toolCalls.add(tc);
                }
            }
        }

        String text = textBuf.toString();
        if (text.isBlank()) {
            message.putNull("content");
        } else {
            message.put("content", text);
        }
        if (!toolCalls.isEmpty()) {
            message.set("tool_calls", toolCalls);
        }

        ObjectNode choice = mapper.createObjectNode();
        choice.put("index", 0);
        choice.set("message", message);
        choice.put("finish_reason", toolCalls.isEmpty() ? "stop" : "tool_calls");
        choices.add(choice);

        openAi.set("choices", choices);
        return openAi;
    }
}
