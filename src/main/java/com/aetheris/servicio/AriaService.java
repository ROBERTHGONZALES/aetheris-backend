package com.aetheris.servicio;

import com.aetheris.dto.AriaHistoryMessage;
import com.aetheris.modelo.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta la conversación con ARIA: llama a Gemini, ejecuta las
 * "herramientas" (function calling) contra los servicios reales de AETHERIS
 * usando el JWT/usuario del llamador, y transmite la respuesta al cliente
 * como eventos Server-Sent Events (text, tool_call, tool_error, done, error).
 */
@Service
@RequiredArgsConstructor
public class AriaService {

    private static final Logger log = LoggerFactory.getLogger(AriaService.class);
    private static final int MAX_TOOL_LOOPS = 5;

    private final GeminiClient geminiClient;
    private final SedeService sedeService;
    private final TransaccionService transaccionService;
    private final AprobacionService aprobacionService;
    private final PresupuestoService presupuestoService;
    private final UsuarioService usuarioService;
    private final ObjectMapper mapper;

    private static final String SYSTEM_INSTRUCTION = """
            Eres ARIA (Asistente de Reportes e Inteligencia de Aetheris), el asistente financiero
            del portal AETHERIS. Respondes siempre en español, de forma clara y profesional.

            Reglas:
            - Para cualquier pregunta sobre datos reales del sistema (sedes, transacciones,
              presupuesto, aprobaciones, usuarios), SIEMPRE usa las herramientas disponibles.
              Nunca inventes cifras.
            - Si necesitas un sedeId, rolId u otro identificador que no tengas, primero usa
              "listar_sedes" (u otra herramienta de listado apropiada) para averiguarlo antes de
              preguntar al usuario.
            - Presenta listas de datos con tablas en formato Markdown cuando tenga sentido.
            - Sé conciso. Si una herramienta falla, explica el problema brevemente sin exponer
              detalles técnicos internos.
            """;

    public void chat(String userMessage, List<AriaHistoryMessage> history, Usuario usuario, SseEmitter emitter) {
        try {
            ArrayNode contents = mapper.createArrayNode();
            if (history != null) {
                for (AriaHistoryMessage h : history) {
                    contents.add(textContent(h.getRole(), h.getText()));
                }
            }
            contents.add(textContent("user", userMessage));

            int loops = 0;
            while (true) {
                ObjectNode body = buildRequestBody(contents);
                JsonNode response = geminiClient.generateContent(body, mapper);
                JsonNode candidate = response.path("candidates").path(0);
                JsonNode contentNode = candidate.path("content");
                JsonNode parts = contentNode.path("parts");

                List<JsonNode> functionCalls = new ArrayList<>();
                StringBuilder textBuilder = new StringBuilder();
                for (JsonNode part : parts) {
                    if (part.has("functionCall")) {
                        functionCalls.add(part.get("functionCall"));
                    } else if (part.has("text")) {
                        textBuilder.append(part.get("text").asText());
                    }
                }

                if (functionCalls.isEmpty() || loops >= MAX_TOOL_LOOPS) {
                    if (textBuilder.length() > 0) {
                        sendEvent(emitter, "text", Map.of("text", textBuilder.toString()));
                    }
                    sendEvent(emitter, "done", Map.of("done", true));
                    emitter.complete();
                    return;
                }

                loops++;
                contents.add(contentNode);

                ObjectNode functionResponseContent = mapper.createObjectNode();
                functionResponseContent.put("role", "function");
                ArrayNode responseParts = mapper.createArrayNode();

                for (JsonNode call : functionCalls) {
                    String name = call.path("name").asText();
                    JsonNode args = call.path("args");
                    sendEvent(emitter, "tool_call",
                            Map.of("name", name, "args", mapper.convertValue(args, Map.class)));

                    ObjectNode functionResponsePart = mapper.createObjectNode();
                    ObjectNode functionResponse = mapper.createObjectNode();
                    functionResponse.put("name", name);
                    ObjectNode responseWrapper = mapper.createObjectNode();
                    try {
                        Object result = executeTool(name, args);
                        responseWrapper.set("result", mapper.valueToTree(result));
                    } catch (Exception e) {
                        log.warn("ARIA tool '{}' failed: {}", name, e.getMessage());
                        sendEvent(emitter, "tool_error", Map.of("name", name,
                                "error", e.getMessage() == null ? "Error desconocido" : e.getMessage()));
                        responseWrapper.put("error", e.getMessage());
                    }
                    functionResponse.set("response", responseWrapper);
                    functionResponsePart.set("functionResponse", functionResponse);
                    responseParts.add(functionResponsePart);
                }

                functionResponseContent.set("parts", responseParts);
                contents.add(functionResponseContent);
            }
        } catch (Exception e) {
            log.error("ARIA chat failed", e);
            try {
                sendEvent(emitter, "error",
                        Map.of("error", e.getMessage() == null ? "Error desconocido" : e.getMessage()));
            } catch (IOException ignored) {
                // best-effort: client likely disconnected already
            }
            emitter.completeWithError(e);
        }
    }

    private Object executeTool(String name, JsonNode args) {
        switch (name) {
            case "listar_sedes":
                return sedeService.listarSedesActivas();
            case "listar_transacciones_sede":
                return transaccionService.listarTransaccionesPorSede(requireArg(args, "sedeId"));
            case "listar_transacciones_pendientes":
                return transaccionService.listarTransaccionesPendientes();
            case "listar_transacciones_periodo":
                return transaccionService.listarTransaccionesPorPeriodo(
                        LocalDate.parse(requireArg(args, "inicio")),
                        LocalDate.parse(requireArg(args, "fin")));
            case "listar_presupuesto":
                return presupuestoService.listarPorSede(requireArg(args, "sedeId"));
            case "listar_aprobaciones_pendientes":
                return aprobacionService.listarFlujosPendientes();
            case "listar_usuarios_por_rol":
                return usuarioService.listarUsuariosPorRol(requireArg(args, "rolId"));
            default:
                throw new RuntimeException("Herramienta desconocida: " + name);
        }
    }

    private String requireArg(JsonNode args, String key) {
        if (!args.has(key) || args.get(key).asText().isBlank()) {
            throw new RuntimeException("Falta el argumento requerido: " + key);
        }
        return args.get(key).asText();
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
    }

    private ObjectNode textContent(String role, String text) {
        ObjectNode content = mapper.createObjectNode();
        content.put("role", role);
        ArrayNode parts = mapper.createArrayNode();
        ObjectNode part = mapper.createObjectNode();
        part.put("text", text);
        parts.add(part);
        content.set("parts", parts);
        return content;
    }

    private ObjectNode buildRequestBody(ArrayNode contents) {
        ObjectNode body = mapper.createObjectNode();
        ObjectNode systemInstruction = mapper.createObjectNode();
        ArrayNode systemParts = mapper.createArrayNode();
        ObjectNode systemTextPart = mapper.createObjectNode();
        systemTextPart.put("text", SYSTEM_INSTRUCTION);
        systemParts.add(systemTextPart);
        systemInstruction.set("parts", systemParts);
        body.set("system_instruction", systemInstruction);
        body.set("contents", contents);
        body.set("tools", buildTools());
        return body;
    }

    private ArrayNode buildTools() {
        ArrayNode tools = mapper.createArrayNode();
        ObjectNode toolsEntry = mapper.createObjectNode();
        ArrayNode declarations = mapper.createArrayNode();

        declarations.add(declaration("listar_sedes",
                "Lista todas las sedes operativas activas de AETHERIS.",
                new LinkedHashMap<>()));

        declarations.add(declaration("listar_transacciones_sede",
                "Lista las transacciones financieras de una sede específica.",
                map("sedeId", "UUID de la sede a consultar.")));

        declarations.add(declaration("listar_transacciones_pendientes",
                "Lista todas las transacciones con estado PENDIENTE, esperando aprobación.",
                new LinkedHashMap<>()));

        declarations.add(declaration("listar_transacciones_periodo",
                "Lista transacciones registradas dentro de un rango de fechas.",
                map("inicio", "Fecha de inicio en formato YYYY-MM-DD.",
                        "fin", "Fecha de fin en formato YYYY-MM-DD.")));

        declarations.add(declaration("listar_presupuesto",
                "Lista las partidas presupuestarias de una sede.",
                map("sedeId", "UUID de la sede a consultar.")));

        declarations.add(declaration("listar_aprobaciones_pendientes",
                "Lista todos los flujos de aprobación de transacciones que aún no han sido resueltos.",
                new LinkedHashMap<>()));

        declarations.add(declaration("listar_usuarios_por_rol",
                "Lista los usuarios del sistema filtrados por un rol específico.",
                map("rolId", "UUID del rol a filtrar.")));

        toolsEntry.set("function_declarations", declarations);
        tools.add(toolsEntry);
        return tools;
    }

    private LinkedHashMap<String, String> map(String... kv) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private ObjectNode declaration(String name, String description, Map<String, String> params) {
        ObjectNode decl = mapper.createObjectNode();
        decl.put("name", name);
        decl.put("description", description);

        ObjectNode parameters = mapper.createObjectNode();
        parameters.put("type", "OBJECT");
        ObjectNode properties = mapper.createObjectNode();
        ArrayNode required = mapper.createArrayNode();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            ObjectNode prop = mapper.createObjectNode();
            prop.put("type", "STRING");
            prop.put("description", entry.getValue());
            properties.set(entry.getKey(), prop);
            required.add(entry.getKey());
        }
        parameters.set("properties", properties);
        if (!params.isEmpty()) {
            parameters.set("required", required);
        }
        decl.set("parameters", parameters);
        return decl;
    }
}
