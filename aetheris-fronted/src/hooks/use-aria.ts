import { useState, useRef, useCallback } from "react";
import { getAuthToken, BACKEND_URL } from "@/lib/api";

export interface AriaMessage {
  id: string;
  role: "user" | "aria";
  text: string;
  toolCalls?: string[];
  error?: boolean;
  streaming?: boolean;
}

interface HistoryEntry {
  role: "user" | "model";
  text: string;
}

export function useAria() {
  const [messages, setMessages] = useState<AriaMessage[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  const buildHistory = useCallback((msgs: AriaMessage[]): HistoryEntry[] => {
    return msgs
      .filter((m) => !m.streaming && !m.error)
      .map((m) => ({ role: m.role === "user" ? "user" : "model", text: m.text }));
  }, []);

  const sendMessage = useCallback(
    async (userText: string) => {
      if (isStreaming || !userText.trim()) return;

      const userMsg: AriaMessage = {
        id: crypto.randomUUID(),
        role: "user",
        text: userText.trim(),
      };

      const ariaId = crypto.randomUUID();
      const ariaMsg: AriaMessage = {
        id: ariaId,
        role: "aria",
        text: "",
        toolCalls: [],
        streaming: true,
      };

      setMessages((prev) => {
        const next = [...prev, userMsg, ariaMsg];
        return next;
      });
      setIsStreaming(true);

      const token = getAuthToken();
      const ctrl = new AbortController();
      abortRef.current = ctrl;

      try {
        // Build history from messages BEFORE this new exchange
        const historySnapshot = buildHistory(
          messages.concat() // current messages (before state update settles)
        );

        const res = await fetch(`${BACKEND_URL}/api/aria/chat`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            message: userText.trim(),
            history: historySnapshot,
          }),
          signal: ctrl.signal,
        });

        if (!res.ok || !res.body) {
          throw new Error(`HTTP ${res.status}`);
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        let fullText = "";
        const toolCallsList: string[] = [];

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split("\n");
          buffer = lines.pop() ?? "";

          for (const line of lines) {
            if (!line.startsWith("data:")) continue;
            const raw = line.slice(5).trim();
            if (!raw) continue;

            try {
              const event = JSON.parse(raw);

              if (event.text !== undefined) {
                fullText += event.text;
                setMessages((prev) =>
                  prev.map((m) =>
                    m.id === ariaId ? { ...m, text: fullText, streaming: true } : m
                  )
                );
              }

              if (event.tool_call) {
                const label = formatToolCall(event.tool_call);
                toolCallsList.push(label);
                setMessages((prev) =>
                  prev.map((m) =>
                    m.id === ariaId ? { ...m, toolCalls: [...toolCallsList], streaming: true } : m
                  )
                );
              }

              if (event.done) {
                setMessages((prev) =>
                  prev.map((m) =>
                    m.id === ariaId
                      ? { ...m, text: fullText || m.text, toolCalls: toolCallsList, streaming: false }
                      : m
                  )
                );
              }

              if (event.error) {
                setMessages((prev) =>
                  prev.map((m) =>
                    m.id === ariaId
                      ? { ...m, text: event.error, error: true, streaming: false }
                      : m
                  )
                );
              }
            } catch {
              // ignore malformed lines
            }
          }
        }

        // Finalize in case done event wasn't received
        setMessages((prev) =>
          prev.map((m) =>
            m.id === ariaId && m.streaming
              ? { ...m, streaming: false, toolCalls: toolCallsList }
              : m
          )
        );
      } catch (err: any) {
        if (err?.name === "AbortError") return;
        setMessages((prev) =>
          prev.map((m) =>
            m.id === ariaId
              ? { ...m, text: "No se pudo conectar con ARIA. Inténtalo de nuevo.", error: true, streaming: false }
              : m
          )
        );
      } finally {
        setIsStreaming(false);
        abortRef.current = null;
      }
    },
    [isStreaming, messages, buildHistory]
  );

  const stop = useCallback(() => {
    abortRef.current?.abort();
  }, []);

  const clear = useCallback(() => {
    abortRef.current?.abort();
    setMessages([]);
    setIsStreaming(false);
  }, []);

  return { messages, isStreaming, sendMessage, stop, clear };
}

function formatToolCall(name: string): string {
  const labels: Record<string, string> = {
    listar_sedes: "Consultando sedes",
    listar_transacciones_sede: "Consultando transacciones",
    listar_transacciones_pendientes: "Consultando pendientes",
    listar_transacciones_periodo: "Consultando período",
    listar_presupuesto: "Consultando presupuesto",
    listar_aprobaciones_pendientes: "Consultando aprobaciones",
    listar_usuarios_por_rol: "Consultando usuarios",
  };
  return labels[name] ?? `Ejecutando ${name}`;
}
