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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Proveedor de respaldo local: Ollama (API compatible con OpenAI).
 *
 * Corre en la máquina del desarrollador y se expone al backend de Railway
 * mediante un túnel de Cloudflare (cloudflared) o ngrok.
 *
 * Configuración en Railway:
 *   OLLAMA_BASE_URL  → URL pública del túnel, ej: https://xyz.trycloudflare.com
 *   OLLAMA_MODEL     → modelo a usar (default: qwen2.5:7b)
 *
 * Si OLLAMA_BASE_URL no está configurada, el proveedor se deshabilita
 * automáticamente y el orquestador lo salta sin error.
 */
@Component
public class OllamaProvider implements AiProvider {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${ollama.base.url:}")
    private String baseUrl;

    @Value("${ollama.model:qwen2.5:7b}")
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

            // Ollama soporta tool calling solo en modelos recientes (llama3.1+, qwen2.5+).
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

            String rawBody = response.body();

            // Intentar parsear como JSON; si falla, construir error legible
            JsonNode json;
            try {
                json = mapper.readTree(rawBody);
            } catch (Exception parseEx) {
                // Respuesta no-JSON (HTML de Cloudflare, texto plano, etc.)
                String preview = rawBody.length() > 200 ? rawBody.substring(0, 200) : rawBody;
                throw new RuntimeException(
                        "Ollama devolvió una respuesta no-JSON (HTTP " + response.statusCode()
                        + "). ¿El túnel está activo y el modelo está descargado? Respuesta: " + preview);
            }

            // Verificar status HTTP DESPUÉS de parsear (Ollama devuelve JSON incluso en errores)
            if (response.statusCode() >= 400) {
                String msg = json.path("error").asText(rawBody);
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

    /**
     * Variante en streaming: pide a Ollama la respuesta con "stream": true y
     * reenvía cada fragmento de texto a {@code onToken} a medida que llega
     * (formato SSE compatible con OpenAI: líneas "data: {...}" terminadas en
     * "data: [DONE]"). Al final reconstruye una respuesta con la misma forma
     * que {@link #chat} (choices[0].message.content / tool_calls) para que el
     * resto del pipeline de function-calling no tenga que cambiar.
     *
     * Esto es lo que hace que, en la web, el texto de ARIA aparezca palabra
     * por palabra en vez de esperar a que Ollama termine de generar todo
     * (que en CPU puede tardar bastante) — igual que se ve al correr
     * "ollama run" en terminal.
     */
    @Override
    public JsonNode chatStream(ArrayNode messages, ArrayNode tools, ObjectMapper mapper,
                                Consumer<String> onToken) {
        if (!isAvailable()) {
            throw new RuntimeException("OLLAMA_BASE_URL no está configurada");
        }

        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.set("messages", messages);
            body.put("temperature", 0.1);
            body.put("stream", true);

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

            HttpResponse<Stream<String>> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() >= 400) {
                String errBody = response.body().collect(Collectors.joining("\n"));
                String preview = errBody.length() > 200 ? errBody.substring(0, 200) : errBody;
                throw new RuntimeException("Ollama error (" + response.statusCode() + "): " + preview);
            }

            StringBuilder fullContent = new StringBuilder();
            // índice de tool_call (según el delta de Ollama) → nodo acumulado
            Map<Integer, ObjectNode> toolCallsByIndex = new LinkedHashMap<>();
            String finishReason = "stop";

            Iterator<String> lines = response.body().iterator();
            while (lines.hasNext()) {
                String line = lines.next();
                if (line.isBlank() || !line.startsWith("data:")) continue;

                String data = line.substring(5).trim();
                if (data.isEmpty() || data.equals("[DONE]")) continue;

                JsonNode chunk;
                try {
                    chunk = mapper.readTree(data);
                } catch (Exception parseEx) {
                    continue; // fragmento no-JSON o parcial, ignorar
                }

                JsonNode choice = chunk.path("choices").path(0);
                JsonNode delta  = choice.path("delta");

                String finish = choice.path("finish_reason").isMissingNode()
                        ? null : choice.path("finish_reason").asText(null);
                if (finish != null && !finish.isBlank()) {
                    finishReason = finish;
                }

                String contentDelta = delta.path("content").asText("");
                if (!contentDelta.isEmpty()) {
                    fullContent.append(contentDelta);
                    onToken.accept(contentDelta);
                }

                JsonNode toolCallsDelta = delta.path("tool_calls");
                if (toolCallsDelta.isArray()) {
                    for (JsonNode tcDelta : toolCallsDelta) {
                        int idx = tcDelta.path("index").asInt(0);
                        ObjectNode acc = toolCallsByIndex.computeIfAbsent(idx, k -> {
                            ObjectNode n = mapper.createObjectNode();
                            n.put("id", "");
                            n.put("type", "function");
                            ObjectNode fn = mapper.createObjectNode();
                            fn.put("name", "");
                            fn.put("arguments", "");
                            n.set("function", fn);
                            return n;
                        });

                        if (tcDelta.hasNonNull("id") && !tcDelta.path("id").asText().isBlank()) {
                            acc.put("id", tcDelta.path("id").asText());
                        }

                        JsonNode fnDelta = tcDelta.path("function");
                        ObjectNode fnAcc = (ObjectNode) acc.get("function");
                        if (fnDelta.hasNonNull("name") && !fnDelta.path("name").asText().isBlank()) {
                            fnAcc.put("name", fnDelta.path("name").asText());
                        }
                        if (fnDelta.hasNonNull("arguments")) {
                            fnAcc.put("arguments",
                                    fnAcc.path("arguments").asText("") + fnDelta.path("arguments").asText(""));
                        }
                    }
                }
            }

            ObjectNode messageNode = mapper.createObjectNode();
            messageNode.put("role", "assistant");
            if (fullContent.length() > 0) {
                messageNode.put("content", fullContent.toString());
            } else {
                messageNode.putNull("content");
            }

            if (!toolCallsByIndex.isEmpty()) {
                ArrayNode toolCallsArr = mapper.createArrayNode();
                for (ObjectNode tc : toolCallsByIndex.values()) {
                    if (tc.path("id").asText("").isBlank()) {
                        tc.put("id", "call_" + UUID.randomUUID());
                    }
                    toolCallsArr.add(tc);
                }
                messageNode.set("tool_calls", toolCallsArr);
            }

            ObjectNode choiceNode = mapper.createObjectNode();
            choiceNode.set("message", messageNode);
            choiceNode.put("finish_reason", finishReason);

            ArrayNode choicesArr = mapper.createArrayNode();
            choicesArr.add(choiceNode);

            ObjectNode result = mapper.createObjectNode();
            result.set("choices", choicesArr);
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Error de red al llamar a Ollama (¿el túnel está activo?): "
                    + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Llamada a Ollama interrumpida", e);
        }
    }
}
