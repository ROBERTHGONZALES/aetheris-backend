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

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orquesta la conversación con ARIA usando Groq (OpenAI-compatible API).
 * Ejecuta function calling contra los servicios reales de AETHERIS y
 * transmite la respuesta al cliente como Server-Sent Events.
 *
 * Las herramientas disponibles se filtran según el rol del usuario logueado:
 *   ADMIN / AUDITOR  → acceso completo (todas las herramientas, incluidas las analíticas)
 *   CONTADOR         → consultas operativas + analíticas (sin usuarios ni aprobaciones)
 *   APROBADOR        → solo pendientes y flujos de aprobación
 */
@Service
@RequiredArgsConstructor
public class AriaService {

    private static final Logger log = LoggerFactory.getLogger(AriaService.class);
    private static final int MAX_TOOL_LOOPS = 5;

    private final GeminiClient geminiClient;   // cliente Groq (nombre heredado)
    private final SedeService sedeService;
    private final TransaccionService transaccionService;
    private final AprobacionService aprobacionService;
    private final PresupuestoService presupuestoService;
    private final UsuarioService usuarioService;
    private final ObjectMapper mapper;

    // ── Permisos de herramientas por rol ───────────────────────────────────

    private static final Map<String, Set<String>> TOOLS_POR_ROL = Map.of(
        "ADMIN", Set.of(
            "listar_sedes",
            "listar_transacciones_sede",
            "listar_transacciones_pendientes",
            "listar_transacciones_periodo",
            "listar_presupuesto",
            "listar_aprobaciones_pendientes",
            "listar_usuarios_por_rol",
            "resumen_mensual_transacciones",
            "resumen_por_sede",
            "resumen_por_categoria"
        ),
        "AUDITOR", Set.of(
            "listar_sedes",
            "listar_transacciones_sede",
            "listar_transacciones_pendientes",
            "listar_transacciones_periodo",
            "listar_presupuesto",
            "listar_aprobaciones_pendientes",
            "listar_usuarios_por_rol",
            "resumen_mensual_transacciones",
            "resumen_por_sede",
            "resumen_por_categoria"
        ),
        "CONTADOR", Set.of(
            "listar_sedes",
            "listar_transacciones_sede",
            "listar_transacciones_periodo",
            "listar_presupuesto",
            "resumen_mensual_transacciones",
            "resumen_por_categoria"
        ),
        "APROBADOR", Set.of(
            "listar_sedes",
            "listar_transacciones_pendientes",
            "listar_aprobaciones_pendientes"
        )
    );

    // ── Prompt dinámico por usuario ────────────────────────────────────────

    private String buildSystemInstruction(Usuario usuario) {
        String rol    = usuario.getRol().getNombre();
        String nombre = usuario.getNombreCompleto();
        return String.format("""
                Eres ARIA (Asistente de Reportes e Inteligencia de Aetheris), el asistente
                financiero del portal AETHERIS. Respondes siempre en español, de forma clara
                y profesional.

                Usuario actual: %s | Rol: %s

                Reglas:
                - Para cualquier pregunta sobre datos reales del sistema (sedes, transacciones,
                  presupuesto, aprobaciones, usuarios) SIEMPRE usa las herramientas disponibles.
                  Nunca inventes cifras.
                - Si necesitas un sedeId u otro identificador que no tengas, usa primero
                  "listar_sedes" (u otra herramienta de listado apropiada) para averiguarlo
                  antes de pedírselo al usuario.
                - Para preguntas de análisis histórico (mes con más ingresos, evolución mensual,
                  comparación de períodos, totales generales) usa "resumen_mensual_transacciones".
                  Para comparar sedes usa "resumen_por_sede". Para analizar categorías de gasto
                  o ingreso usa "resumen_por_categoria". Estas herramientas devuelven datos
                  agrupados del historial completo.
                - Presenta tablas en formato Markdown cuando la respuesta tenga más de 3 filas.
                - Sé conciso. Si una herramienta falla, explica el problema brevemente sin
                  exponer detalles técnicos internos.
                - Solo puedes usar las herramientas asignadas a tu rol (%s). No improvises
                  información que no provenga de una herramienta disponible.
                """, nombre, rol, rol);
    }

    // ── Punto de entrada principal ─────────────────────────────────────────

    /**
     * Mantiene la sesión de Hibernate abierta durante todo el chat para que
     * las relaciones LAZY de las entidades (usuarioRegistro, etc.) puedan
     * inicializarse cuando Jackson las serializa fuera del método de servicio.
     */
    @Transactional(readOnly = true)
    public void chat(String userMessage, List<AriaHistoryMessage> history,
                     Usuario usuario, SseEmitter emitter) {
        try {
            ArrayNode messages = mapper.createArrayNode();

            // Sistema con contexto del usuario y su rol
            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", buildSystemInstruction(usuario));
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

            // Herramientas filtradas por rol
            String    rol   = usuario.getRol().getNombre();
            ArrayNode tools = buildTools(rol);

            // ── Bucle de function calling ───────────────────────────────────
            int loops = 0;
            while (true) {
                JsonNode response  = geminiClient.chat(messages, tools, mapper);
                JsonNode choice    = response.path("choices").path(0);
                JsonNode message   = choice.path("message");
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
                        Object   result = executeTool(toolName, args, usuario);
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

    private Object executeTool(String name, JsonNode args, Usuario usuario) {
        String       rol     = usuario.getRol().getNombre();
        Set<String>  allowed = TOOLS_POR_ROL.getOrDefault(rol, Set.of());

        // Verificación de acceso por rol (segunda línea de defensa)
        if (!allowed.contains(name)) {
            throw new RuntimeException(
                    "Tu rol (" + rol + ") no tiene acceso a la herramienta: " + name);
        }

        return switch (name) {
            // ── Herramientas base ──────────────────────────────────────────
            case "listar_sedes"
                    -> sedeService.listarSedesActivas();
            case "listar_transacciones_sede"
                    -> transaccionService.listarTransaccionesPorSede(requireArg(args, "sedeId"));
            case "listar_transacciones_pendientes"
                    -> transaccionService.listarTransaccionesPendientes();
            case "listar_transacciones_periodo"
                    -> transaccionService.listarTransaccionesPorPeriodo(
                            LocalDate.parse(requireArg(args, "inicio")),
                            LocalDate.parse(requireArg(args, "fin")));
            case "listar_presupuesto"
                    -> presupuestoService.listarPorSede(requireArg(args, "sedeId"));
            case "listar_aprobaciones_pendientes"
                    -> aprobacionService.listarFlujosPendientes();
            case "listar_usuarios_por_rol"
                    -> usuarioService.listarUsuariosPorRol(requireArg(args, "rolId"));
            // ── Herramientas analíticas ────────────────────────────────────
            case "resumen_mensual_transacciones"
                    -> transaccionService.resumenMensual();
            case "resumen_por_sede"
                    -> transaccionService.resumenPorSede();
            case "resumen_por_categoria"
                    -> transaccionService.resumenPorCategoria();
            default -> throw new RuntimeException("Herramienta desconocida: " + name);
        };
    }

    private String requireArg(JsonNode args, String key) {
        if (!args.has(key) || args.get(key).asText().isBlank()) {
            throw new RuntimeException("Falta el argumento requerido: " + key);
        }
        return args.get(key).asText();
    }

    // ── Construcción del set de herramientas filtrado por rol ───────────────

    private ArrayNode buildTools(String rol) {
        Set<String> allowed = TOOLS_POR_ROL.getOrDefault(rol, Set.of());
        ArrayNode   tools   = mapper.createArrayNode();

        // Herramientas base
        addIfAllowed(tools, allowed, tool(
                "listar_sedes",
                "Lista todas las sedes operativas activas de AETHERIS.",
                map()));

        addIfAllowed(tools, allowed, tool(
                "listar_transacciones_sede",
                "Lista las transacciones financieras de una sede específica.",
                map("sedeId", "UUID de la sede a consultar.")));

        addIfAllowed(tools, allowed, tool(
                "listar_transacciones_pendientes",
                "Lista todas las transacciones con estado PENDIENTE, esperando aprobación.",
                map()));

        addIfAllowed(tools, allowed, tool(
                "listar_transacciones_periodo",
                "Lista transacciones registradas dentro de un rango de fechas.",
                map("inicio", "Fecha de inicio en formato YYYY-MM-DD.",
                    "fin",    "Fecha de fin en formato YYYY-MM-DD.")));

        addIfAllowed(tools, allowed, tool(
                "listar_presupuesto",
                "Lista las partidas presupuestarias de una sede específica.",
                map("sedeId", "UUID de la sede.")));

        addIfAllowed(tools, allowed, tool(
                "listar_aprobaciones_pendientes",
                "Lista todos los flujos de aprobación de transacciones sin resolver.",
                map()));

        addIfAllowed(tools, allowed, tool(
                "listar_usuarios_por_rol",
                "Lista los usuarios del sistema filtrados por un rol específico.",
                map("rolId", "UUID del rol a filtrar.")));

        // Herramientas analíticas
        addIfAllowed(tools, allowed, tool(
                "resumen_mensual_transacciones",
                "Muestra el resumen de ingresos y egresos agrupado por mes y año de todo el " +
                "historial. Úsala para responder: ¿qué mes tuvo más ingresos?, ¿cómo ha " +
                "evolucionado el flujo mensual?, ¿cuál fue el mejor/peor mes?, " +
                "¿cuánto se ingresó/egresó en total?",
                map()));

        addIfAllowed(tools, allowed, tool(
                "resumen_por_sede",
                "Muestra los totales de ingresos y egresos agrupados por cada sede. " +
                "Útil para comparar el desempeño financiero entre sedes de la empresa.",
                map()));

        addIfAllowed(tools, allowed, tool(
                "resumen_por_categoria",
                "Muestra los totales de ingresos y egresos agrupados por categoría contable. " +
                "Útil para analizar en qué categorías se concentra el gasto o el ingreso.",
                map()));

        return tools;
    }

    private void addIfAllowed(ArrayNode tools, Set<String> allowed, ObjectNode toolDef) {
        String name = toolDef.path("function").path("name").asText();
        if (allowed.contains(name)) {
            tools.add(toolDef);
        }
    }

    /** Construye una herramienta en formato OpenAI tool-calling. */
    private ObjectNode tool(String name, String description, Map<String, String> params) {
        ObjectNode t = mapper.createObjectNode();
        t.put("type", "function");

        ObjectNode function = mapper.createObjectNode();
        function.put("name", name);
        function.put("description", description);

        ObjectNode parameters = mapper.createObjectNode();
        parameters.put("type", "object");
        ObjectNode properties = mapper.createObjectNode();
        ArrayNode  required   = mapper.createArrayNode();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            ObjectNode prop = mapper.createObjectNode();
            prop.put("type", "string");
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
