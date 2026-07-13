package com.aetheris.servicio;

import com.aetheris.servicio.provider.AiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquestador de proveedores de IA para ARIA.
 *
 * Intenta los proveedores en el orden configurado por ARIA_PROVIDER_ORDER
 * (por defecto: groq → gemini → openrouter). Si uno falla (429, timeout,
 * error de API, key no configurada), pasa automáticamente al siguiente.
 *
 * Variables de entorno opcionales en Railway:
 *   ARIA_PROVIDER_ORDER=groq,gemini,openrouter
 */
@Service
public class AriaOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AriaOrchestratorService.class);

    private final Map<String, AiProvider> providerByName;
    private final List<String>            providerOrder;

    public AriaOrchestratorService(
            List<AiProvider> providers,
            @Value("${aria.provider.order:groq,gemini,openrouter}") String orderCsv) {

        this.providerByName = providers.stream()
                .collect(Collectors.toMap(p -> p.getName().toLowerCase(), Function.identity()));

        this.providerOrder = new ArrayList<>();
        for (String name : orderCsv.split(",")) {
            providerOrder.add(name.trim().toLowerCase());
        }
    }

    /**
     * Envía la petición al primer proveedor disponible.
     * Si falla, intenta el siguiente. Lanza excepción solo si todos fallan.
     */
    public JsonNode chat(ArrayNode messages, ArrayNode tools, ObjectMapper mapper) {
        List<String> errors = new ArrayList<>();

        for (String name : providerOrder) {
            AiProvider provider = providerByName.get(name);
            if (provider == null) {
                log.debug("Proveedor '{}' desconocido en ARIA_PROVIDER_ORDER, ignorando.", name);
                continue;
            }
            if (!provider.isAvailable()) {
                log.debug("Proveedor '{}' sin API key configurada, saltando.", provider.getName());
                continue;
            }
            try {
                log.info("ARIA → proveedor activo: {}", provider.getName());
                return provider.chat(messages, tools, mapper);
            } catch (Exception e) {
                String err = provider.getName() + ": " + e.getMessage();
                log.warn("ARIA fallback — {} falló, probando siguiente. Causa: {}", provider.getName(), e.getMessage());
                errors.add(err);
            }
        }

        throw new RuntimeException(
                "Todos los proveedores de IA fallaron. Detalles: " + String.join(" | ", errors));
    }

    /**
     * Igual que {@link #chat}, pero en modo streaming: {@code onToken} se
     * invoca con cada fragmento de texto a medida que el proveedor activo lo
     * va generando (si lo soporta). Mantiene el mismo mecanismo de fallback
     * entre proveedores.
     */
    public JsonNode chatStream(ArrayNode messages, ArrayNode tools, ObjectMapper mapper,
                                Consumer<String> onToken) {
        List<String> errors = new ArrayList<>();

        for (String name : providerOrder) {
            AiProvider provider = providerByName.get(name);
            if (provider == null) {
                log.debug("Proveedor '{}' desconocido en ARIA_PROVIDER_ORDER, ignorando.", name);
                continue;
            }
            if (!provider.isAvailable()) {
                log.debug("Proveedor '{}' sin API key configurada, saltando.", provider.getName());
                continue;
            }
            try {
                log.info("ARIA (stream) → proveedor activo: {}", provider.getName());
                return provider.chatStream(messages, tools, mapper, onToken);
            } catch (Exception e) {
                String err = provider.getName() + ": " + e.getMessage();
                log.warn("ARIA fallback — {} falló, probando siguiente. Causa: {}", provider.getName(), e.getMessage());
                errors.add(err);
            }
        }

        throw new RuntimeException(
                "Todos los proveedores de IA fallaron. Detalles: " + String.join(" | ", errors));
    }
}
