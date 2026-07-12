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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta la conversación con ARIA usando Groq (OpenAI-compatible API).
 * Ejecuta function calling contra los servicios reales de AETHERIS y
 * transmite la respuesta al cliente como Server-Sent Events.
 */
@Service
@RequiredArgsConstructor
public class AriaService {

    private static final Logger log = LoggerFactory.getLogger(AriaService.class);
    private static final int MAX_TOOL_LOOPS = 5;

    private final GeminiClient geminiClient;   // ahora llama a Groq internamente
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

    public void chat(String userMessage, List<AriaHistoryMessage> history,
                     Usuario usuario, SseEmitter emitter) {
        try {
            // ── Construir lista de mensajes en formato OpenAI ──────────────
            ArrayNode messages = mapper.createArrayNode();

            // Mensaje de sistema
            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_INSTRUCTION);
            messages.add(systemMsg);

            // Historial previo
            if (history != null) {
                for (AriaHistoryMessage h : history) {
                    ObjectNode msg = mapper.createObjectNode();
                    msg.put("role", "user".equals(h.getRole()) ? "user" : "assistant");
                    msg.put("content", h.getText());
                    messages.add(msg);
                }
            }

            // Mensaje del usuario actual
            ObjectNode userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            ArrayNode tools = buildTools();

            // ── Bucle de function calling ───────────────────────────────────
            int loops = 0;
            while (true) {
                JsonNode response = geminiClient.chat(messages, tools, mapper);
                JsonNode choice = response.path("choices").path(0);
                JsonNode message = choice.path("message");
                JsonNode toolCalls = message.path("tool_calls");

                boolean hasToolCalls = toolCalls.isArray() && !toolCalls.isEmpty();

                if (!hasToolCalls || loops >= MAX_TOOL_LOOPS) {
                    // Respuesta final de texto
                    String text = message.path("content").asText("");
                    if (!text.isBlank()) {
                        sendEvent(emitter, "text", Map.of("text", text));
                    }
                    sendEvent(emitter, "done", Map.of("done", true));
                    emitter.complete();
                    return;
                }

                // Añadir mensaje del asistente con tool_calls al historial
                ObjectNode assistantMsg = mapper.createObjectNode();
                assistantMsg.put("role", "assistant");
                String assistantContent = message.path("content").asText("");
                if (assistantContent.isBlank()) {
                    assistantMsg.putNull("content");
                } else {
                    assistantMsg.put("content", assistantContent);
                }
                assistantMsg.set("tool_calls", toolCalls);
                messages.add(assistantMsg);

                // Ejecutar cada tool call y añadir resultado
                for (JsonNode toolCall : toolCalls) {
                    String toolCallId = toolCall.path("id").asText();
                    String toolName   = toolCall.path("function").path("name").asText();
                    String argsStr    = toolCall.path("function").path("arguments").asText("{}");

                    sendEvent(emitter, "tool_call",
                            Map.of("name", toolName, "args", argsStr));

                    ObjectNode toolResultMsg = mapper.createObjectNode();
                    toolResultMsg.put("role", "tool");
                    toolResultMsg.put("tool_call_id", toolCallId);

                    try {
                        JsonNode args   = mapper.readTree(argsStr);
                        Object   result = executeTool(toolName, args);
                        toolResultMsg.put("content", mapper.writeValueAsString(result));
                    } catch (Exception e) {
                        log.warn("ARIA tool '{}' failed: {}", toolName, e.getMessage());
                        sendEvent(emitter, "tool_error",
                                Map.of("name", toolName,
                                       "error", e.getMessage() != null
                                               ? e.getMessage() : "Error desconocido"));
                        toolResultMsg.put("content",
                                "{\"error\":\"" + e.getMessage() + "\"}");
                    }
                    messages.add(toolResultMsg);
                }

                loops++;
            }

        } catch (Exception e) {
            log.error("ARIA chat failed", e);
            try {
                sendEvent(emitter, "error",
                        Map.of("error", e.getMessage() != null
                                ? e.getMessage() : "Error desconocido"));
            } catch (IOException ignored) { }
            emitter.completeWithError(e);
        }
    }

    // ── Ejecución de herramientas ───────────────────────────────────────────

    private Object executeTool(String name, JsonNode args) {
        return switch (name) {
            case "listar_sedes"                  -> sedeService.listarSedesActivas();
            case "listar_transacciones_sede"     -> transaccionService
                    .listarTransaccionesPorSede(requireArg(args, "sedeId"));
            case "listar_transacciones_pendientes" -> transaccionService
                    .listarTransaccionesPendientes();
            case "listar_transacciones_periodo"  -> transaccionService
                    .listarTransaccionesPorPeriodo(
                            LocalDate.parse(requireArg(args, "inicio")),
                            LocalDate.parse(requireArg(args, "fin")));
            case "listar_presupuesto"            -> presupuestoService
                    .listarPorSede(requireArg(args, "sedeId"));
            case "listar_aprobaciones_pendientes" -> aprobacionService
                    .listarFlujosPendientes();
            case "listar_usuarios_por_rol"       -> usuarioService
                    .listarUsuariosPorRol(requireArg(args, "rolId"));
            default -> throw new RuntimeException("Herramienta desconocida: " + name);
        };
    }

    private String requireArg(JsonNode args, String key) {
        if (!args.has(key) || args.get(key).asText().isBlank()) {
            throw new RuntimeException("Falta el argumento requerido: " + key);
        }
        return args.get(key).asText();
    }

    // ── Construcción de herramientas en formato OpenAI ──────────────────────

    private ArrayNode buildTools() {
        ArrayNode tools = mapper.createArrayNode();

        tools.add(tool("listar_sedes",
                "Lista todas las sedes operativas activas de AETHERIS.",
                new LinkedHashMap<>()));

        tools.add(tool("listar_transacciones_sede",
                "Lista las transacciones financieras de una sede específica.",
                map("sedeId", "UUID de la sede a consultar.")));

        tools.add(tool("listar_transacciones_pendientes",
                "Lista todas las transacciones con estado PENDIENTE, esperando aprobación.",
                new LinkedHashMap<>()));

        tools.add(tool("listar_transacciones_periodo",
                "Lista transacciones registradas dentro de un rango de fechas.",
                map("inicio", "Fecha de inicio en formato YYYY-MM-DD.",
                    "fin",    "Fecha de fin en formato YYYY-MM-DD.")));

        tools.add(tool("listar_presupuesto",
                "Lista las partidas presupuestarias de una sede.",
                map("sedeId", "UUID de la sede a consultar.")));

        tools.add(tool("listar_aprobaciones_pendientes",
                "Lista todos los flujos de aprobación de transacciones sin resolver.",
                new LinkedHashMap<>()));

        tools.add(tool("listar_usuarios_por_rol",
                "Lista los usuarios del sistema filtrados por un rol específico.",
                map("rolId", "UUID del rol a filtrar.")));

        return tools;
    }

    /** Construye una herramienta en formato OpenAI tool-calling. */
    private ObjectNode tool(String name, String description, Map<String, String> params) {
        ObjectNode t = mapper.createObjectNode();
        t.put("type", "function");

        ObjectNode function = mapper.createObjectNode();
        function.put("name", name);
        function.put("description", description);

        ObjectNode parameters = mapper.createObjectNode();
        parameters.put("type", "object");          // OpenAI usa minúsculas
        ObjectNode properties = mapper.createObjectNode();
        ArrayNode  required   = mapper.createArrayNode();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            ObjectNode prop = mapper.createObjectNode();
            prop.put("type", "string");            // OpenAI usa minúsculas
            prop.put("description", entry.getValue());
            properties.set(entry.getKey(), prop);
            required.add(entry.getKey());
        }
        parameters.set("properties", properties);
        if (!params.isEmpty()) {
            parameters.set("required", required);
        }
        function.set("parameters", parameters);
        t.set("function", function);
        return t;
    }

    private LinkedHashMap<String, String> map(String... kv) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data)
            throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
    }
}
