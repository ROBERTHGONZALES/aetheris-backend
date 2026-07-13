import { useRef, useEffect, type KeyboardEvent } from "react";
import { useState } from "react";
import { AppLayout } from "@/components/layout";
import { useAria, type AriaMessage } from "@/hooks/use-aria";
import { cn } from "@/lib/utils";
import { Bot, Send, Square, Trash2, Loader2, Wrench, AlertCircle } from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

const SUGGESTIONS = [
  "Dame un resumen financiero de todo el historial",
  "¿Cuál fue el mes con más ingresos?",
  "Compara ingresos y egresos por sede",
  "¿Qué categorías tienen más gasto?",
  "Muéstrame las transacciones pendientes de aprobación",
  "¿Cómo está el presupuesto actual?",
];

export default function AriaPage() {
  const [input, setInput] = useState("");
  const { messages, isStreaming, sendMessage, stop, clear } = useAria();
  const bottomRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const isEmpty = messages.length === 0;

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    const ta = textareaRef.current;
    if (!ta) return;
    ta.style.height = "auto";
    ta.style.height = Math.min(ta.scrollHeight, 160) + "px";
  }, [input]);

  const submit = () => {
    if (!input.trim() || isStreaming) return;
    sendMessage(input);
    setInput("");
  };

  const onKey = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  };

  return (
    <AppLayout>
      <div className="flex flex-col h-[calc(100vh-4rem)] max-w-4xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6 shrink-0">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary">
              <Bot className="h-5 w-5 text-primary-foreground" />
            </div>
            <div>
              <h1 className="text-xl font-semibold text-foreground">ARIA</h1>
              <p className="text-sm text-muted-foreground">Asistente de Reportes e Inteligencia de Aetheris</p>
            </div>
          </div>
          {!isEmpty && (
            <button
              onClick={clear}
              className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm text-muted-foreground border border-border hover:bg-muted hover:text-foreground transition-colors"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Limpiar
            </button>
          )}
        </div>

        {/* Messages area */}
        <div className="flex-1 overflow-y-auto rounded-xl border border-border bg-card">
          {isEmpty ? (
            <div className="flex flex-col items-center justify-center h-full gap-6 p-8">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
                <Bot className="h-8 w-8 text-primary" />
              </div>
              <div className="text-center">
                <p className="text-base font-medium text-foreground">¿En qué puedo ayudarte?</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Consulto datos reales del sistema. Las tablas y análisis se muestran con formato completo.
                </p>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full max-w-xl">
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    onClick={() => { sendMessage(s); }}
                    className="rounded-lg border border-border bg-background px-4 py-2.5 text-left text-sm text-foreground/70 hover:border-primary/40 hover:text-foreground hover:bg-muted/50 transition-colors"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-6 p-6">
              {messages.map((msg) => (
                <FullMessageBubble key={msg.id} msg={msg} />
              ))}
              <div ref={bottomRef} />
            </div>
          )}
        </div>

        {/* Input */}
        <div className="mt-4 shrink-0">
          <div className="flex items-end gap-3 rounded-xl border border-border bg-card px-4 py-3 focus-within:border-primary/50 transition-colors shadow-sm">
            <textarea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={onKey}
              placeholder="Pregunta algo a ARIA… (Enter para enviar, Shift+Enter para nueva línea)"
              rows={1}
              disabled={isStreaming}
              className="flex-1 resize-none bg-transparent text-sm text-foreground placeholder:text-muted-foreground outline-none disabled:opacity-50"
              style={{ maxHeight: 160 }}
            />
            <button
              onClick={isStreaming ? stop : submit}
              disabled={!isStreaming && !input.trim()}
              className={cn(
                "flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg transition-colors disabled:opacity-30 disabled:cursor-not-allowed",
                isStreaming
                  ? "bg-destructive/10 text-destructive hover:bg-destructive/20"
                  : "bg-primary text-primary-foreground hover:bg-primary/90"
              )}
            >
              {isStreaming
                ? <Square className="h-4 w-4 fill-current" />
                : <Send className="h-4 w-4" />}
            </button>
          </div>
          <p className="mt-2 text-center text-xs text-muted-foreground/60">
            ARIA usa datos reales del sistema · Las respuestas pueden tardar unos segundos
          </p>
        </div>
      </div>
    </AppLayout>
  );
}

/* ─── Bubble para la vista de página completa ───────────────── */
function FullMessageBubble({ msg }: { msg: AriaMessage }) {
  const isUser = msg.role === "user";

  return (
    <div className={cn("flex gap-3", isUser ? "flex-row-reverse" : "flex-row")}>
      {!isUser && (
        <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-primary mt-1">
          <Bot className="h-4 w-4 text-primary-foreground" />
        </div>
      )}

      <div className={cn("flex flex-col gap-2", isUser ? "items-end max-w-[70%]" : "flex-1")}>
        {/* Tool calls */}
        {!isUser && msg.toolCalls && msg.toolCalls.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {msg.toolCalls.map((tc, i) => (
              <div key={i} className="flex items-center gap-1.5 rounded-md bg-muted px-2.5 py-1 text-xs text-muted-foreground">
                <Wrench className="h-3 w-3 flex-shrink-0" />
                <span>{tc}…</span>
              </div>
            ))}
          </div>
        )}

        {/* Bubble */}
        {(msg.text || msg.streaming) && (
          <div
            className={cn(
              "rounded-2xl px-4 py-3 text-sm leading-relaxed",
              isUser
                ? "bg-primary text-primary-foreground rounded-tr-sm"
                : msg.error
                ? "bg-destructive/10 text-destructive rounded-tl-sm border border-destructive/20"
                : "bg-muted text-foreground rounded-tl-sm w-full"
            )}
          >
            {msg.error && (
              <div className="flex items-center gap-1.5 mb-2">
                <AlertCircle className="h-4 w-4" />
                <span className="text-xs font-medium">Error</span>
              </div>
            )}

            {isUser ? (
              <span className="whitespace-pre-wrap">{msg.text}</span>
            ) : (
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                  table: ({ children }) => (
                    <div className="overflow-x-auto my-3 rounded-lg border border-border">
                      <table className="w-full text-sm border-collapse">{children}</table>
                    </div>
                  ),
                  thead: ({ children }) => (
                    <thead className="bg-muted-foreground/10 border-b border-border">{children}</thead>
                  ),
                  th: ({ children }) => (
                    <th className="px-4 py-2.5 text-left font-semibold text-foreground text-xs uppercase tracking-wider">{children}</th>
                  ),
                  td: ({ children }) => (
                    <td className="px-4 py-2.5 border-t border-border/50 text-foreground/90">{children}</td>
                  ),
                  tr: ({ children }) => (
                    <tr className="hover:bg-muted/30 transition-colors">{children}</tr>
                  ),
                  strong: ({ children }) => (
                    <strong className="font-semibold text-foreground">{children}</strong>
                  ),
                  ul: ({ children }) => (
                    <ul className="list-disc list-inside space-y-1 my-2 text-foreground/90">{children}</ul>
                  ),
                  ol: ({ children }) => (
                    <ol className="list-decimal list-inside space-y-1 my-2 text-foreground/90">{children}</ol>
                  ),
                  p: ({ children }) => (
                    <p className="mb-2 last:mb-0 text-foreground/90">{children}</p>
                  ),
                  h3: ({ children }) => (
                    <h3 className="text-base font-semibold text-foreground mt-3 mb-1">{children}</h3>
                  ),
                  blockquote: ({ children }) => (
                    <blockquote className="border-l-2 border-primary/40 pl-3 italic text-muted-foreground my-2">{children}</blockquote>
                  ),
                }}
              >
                {msg.text}
              </ReactMarkdown>
            )}

            {msg.streaming && !msg.text && (
              <span className="flex items-center gap-2 text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" />
                <span className="text-sm">ARIA está pensando…</span>
              </span>
            )}
            {msg.streaming && msg.text && (
              <span className="ml-1 inline-block h-4 w-0.5 animate-pulse bg-current align-middle opacity-70" />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
