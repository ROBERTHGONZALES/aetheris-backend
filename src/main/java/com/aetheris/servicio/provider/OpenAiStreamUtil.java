package com.aetheris.servicio.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Utilidad compartida para consumir respuestas en streaming (Server-Sent
 * Events) de APIs compatibles con OpenAI chat/completions — usada tanto por
 * Groq como por Ollama (ambas exponen /v1/chat/completions con stream:true).
 *
 * Reconstruye un JsonNode equivalente a la respuesta NO-streaming
 * (choices[0].message.content + tool_calls) mientras invoca onDelta(texto)
 * por cada fragmento de contenido recibido, permitiendo transmitir texto
 * en tiempo real hacia el cliente sin perder la lógica de function calling
 * que necesita ver la respuesta completa al final.
 */
final class OpenAiStreamUtil {

    private OpenAiStreamUtil() {}

    static JsonNode consume(Stream<String> lines, ObjectMapper mapper, Consumer<String> onDelta) {
        StringBuilder fullContent = new StringBuilder();
        Map<Integer, ObjectNode> toolCallsByIndex = new LinkedHashMap<>();
        String finishReason = "stop";

        var it = lines.iterator();
        while (it.hasNext()) {
            String line = it.next();
            if (line == null || line.isBlank() || !line.startsWith("data:")) continue;

            String raw = line.substring(5).trim();
            if (raw.equals("[DONE]")) break;

            JsonNode chunk;
            try {
                chunk = mapper.readTree(raw);
            } catch (Exception e) {
                continue; // línea parcial/mal formada — se ignora
            }

            JsonNode choice = chunk.path("choices").path(0);
            JsonNode delta  = choice.path("delta");

            String contentDelta = delta.path("content").asText("");
            if (!contentDelta.isEmpty()) {
                fullContent.append(contentDelta);
                onDelta.accept(contentDelta);
            }

            JsonNode toolCallDeltas = delta.path("tool_calls");
            if (toolCallDeltas.isArray()) {
                for (JsonNode tc : toolCallDeltas) {
                    int idx = tc.path("index").asInt(0);
                    ObjectNode acc = toolCallsByIndex.computeIfAbsent(idx, i -> {
                        ObjectNode n = mapper.createObjectNode();
                        n.put("id", "");
                        n.put("type", "function");
                        ObjectNode fn = mapper.createObjectNode();
                        fn.put("name", "");
                        fn.put("arguments", "");
                        n.set("function", fn);
                        return n;
                    });
                    if (!tc.path("id").asText("").isEmpty()) {
                        acc.put("id", tc.path("id").asText());
                    }
                    JsonNode fnDelta = tc.path("function");
                    ObjectNode fn = (ObjectNode) acc.get("function");
                    if (!fnDelta.path("name").asText("").isEmpty()) {
                        fn.put("name", fnDelta.path("name").asText());
                    }
                    if (fnDelta.has("arguments")) {
                        fn.put("arguments", fn.path("arguments").asText("") + fnDelta.path("arguments").asText(""));
                    }
                }
            }

            String fr = choice.path("finish_reason").asText("");
            if (!fr.isEmpty()) finishReason = fr;
        }

        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");
        message.put("content", fullContent.toString());

        if (!toolCallsByIndex.isEmpty()) {
            ArrayNode toolCallsArr = mapper.createArrayNode();
            toolCallsByIndex.values().forEach(toolCallsArr::add);
            message.set("tool_calls", toolCallsArr);
        }

        ObjectNode choiceOut = mapper.createObjectNode();
        choiceOut.put("finish_reason", finishReason);
        choiceOut.set("message", message);

        ArrayNode choicesArr = mapper.createArrayNode();
        choicesArr.add(choiceOut);

        ObjectNode result = mapper.createObjectNode();
        result.set("choices", choicesArr);
        return result;
    }
}
