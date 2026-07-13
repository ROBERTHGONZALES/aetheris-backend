import { useState, useRef, useEffect, type KeyboardEvent } from "react";
import { Bot, X, Send, Square, Trash2, ChevronDown, Loader2, Wrench, AlertCircle } from "lucide-react";
import { useAria, type AriaMessage } from "@/hooks/use-aria";
import { cn } from "@/lib/utils";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

/* ─── Welcome suggestions ─────────────────────────────────────── */
const SUGGESTIONS = [
  "¿Cuántas transacciones están pendientes?",
  "Muéstrame el presupuesto por sede",
  "¿Qué aprobaciones hay sin resolver?",
  "Lista las sedes activas",
];

export function AriaChat() {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [atBottom, setAtBottom] = useState(true);
  const { messages, isStreaming, sendMessage, stop, clear } = useAria();
  const bottomRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  /* auto-scroll */
  useEffect(() => {
    if (atBottom) bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, atBottom]);

  /* track scroll position */
  const handleScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    setAtBottom(el.scrollHeight - el.scrollTop - el.clientHeight < 60);
  };

  /* auto-resize textarea */
  useEffect(() => {
    const ta = textareaRef.current;
    if (!ta) return;
    ta.style.height = "auto";
    ta.style.height = Math.min(ta.scrollHeight, 120) + "px";
  }, [input]);

  const submit = () => {
    if (!input.trim() || isStreaming) return;
    sendMessage(input);
    setInput("");
    setAtBottom(true);
  };

  const onKey = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  };

  const isEmpty = messages.length === 0;

  return (
    <>
      {/* Floating button */}
      <button
        onClick={() => setOpen((v) => !v)}
        className={cn(
          "fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full shadow-2xl transition-all duration-200 hover:scale-105 active:scale-95",
          open
            ? "bg-foreground text-background"
            : "bg-primary text-primary-foreground hover:bg-primary/90"
        )}
        aria-label="Abrir ARIA"
      >
        {open ? <X className="h-5 w-5" /> : <Bot className="h-5 w-5" />}
      </button>

      {/* Panel */}
      <div
        className={cn(
          "fixed bottom-24 right-6 z-50 flex w-[380px] flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-2xl transition-all duration-300 origin-bottom-right",
          open ? "opacity-100 scale-100 pointer-events-auto" : "opacity-0 scale-95 pointer-events-none"
        )}
        style={{ maxHeight: "min(600px, calc(100vh - 120px))" }}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border bg-sidebar px-4 py-3 shrink-0">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary">
              <Bot className="h-4 w-4 text-primary-foreground" />
            </div>
            <div>
              <p className="text-sm font-semibold text-sidebar-foreground">ARIA</p>
              <p className="text-xs text-sidebar-foreground/60">Asistente financiero</p>
            </div>
          </div>
          <div className="flex items-center gap-1">
            {messages.length > 0 && (
              <button
                onClick={clear}
                className="rounded-md p-1.5 text-sidebar-foreground/50 hover:bg-sidebar-accent hover:text-sidebar-foreground transition-colors"
                title="Limpiar conversación"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            )}
            <button
              onClick={() => setOpen(false)}
              className="rounded-md p-1.5 text-sidebar-foreground/50 hover:bg-sidebar-accent hover:text-sidebar-foreground transition-colors"
            >
              <ChevronDown className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* Messages */}
        <div
          ref={scrollRef}
          onScroll={handleScroll}
          className="flex-1 overflow-y-auto px-4 py-4 space-y-4"
          style={{ minHeight: 200 }}
        >
          {isEmpty ? (
            <div className="flex flex-col items-center gap-4 py-6">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
                <Bot className="h-6 w-6 text-primary" />
              </div>
              <div className="text-center">
                <p className="text-sm font-medium text-foreground">Hola, soy ARIA</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Tu asistente de control financiero. Consulto datos en tiempo real.
                </p>
              </div>
              <div className="grid grid-cols-1 gap-2 w-full">
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    onClick={() => { setInput(s); textareaRef.current?.focus(); }}
                    className="rounded-lg border border-border bg-background px-3 py-2 text-left text-xs text-foreground/70 hover:border-primary/40 hover:text-foreground transition-colors"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((msg) => <MessageBubble key={msg.id} msg={msg} />)
          )}
          <div ref={bottomRef} />
        </div>

        {/* Scroll-to-bottom hint */}
        {!atBottom && (
          <button
            onClick={() => { setAtBottom(true); bottomRef.current?.scrollIntoView({ behavior: "smooth" }); }}
            className="absolute bottom-20 right-6 flex h-7 w-7 items-center justify-center rounded-full border border-border bg-card shadow-md text-muted-foreground hover:text-foreground transition-colors"
          >
            <ChevronDown className="h-3.5 w-3.5" />
          </button>
        )}

        {/* Input */}
        <div className="border-t border-border bg-card px-3 py-3 shrink-0">
          <div className="flex items-end gap-2 rounded-xl border border-border bg-background px-3 py-2 focus-within:border-primary/50 transition-colors">
            <textarea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={onKey}
              placeholder="Pregunta algo a ARIA…"
              rows={1}
              disabled={isStreaming}
              className="flex-1 resize-none bg-transparent text-sm text-foreground placeholder:text-muted-foreground outline-none disabled:opacity-50"
              style={{ maxHeight: 120 }}
            />
            <button
              onClick={isStreaming ? stop : submit}
              disabled={!isStreaming && !input.trim()}
              className={cn(
                "flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg transition-colors disabled:opacity-30 disabled:cursor-not-allowed",
                isStreaming
                  ? "bg-destructive/10 text-destructive hover:bg-destructive/20"
                  : "bg-primary text-primary-foreground hover:bg-primary/90"
              )}
            >
              {isStreaming
                ? <Square className="h-3.5 w-3.5 fill-current" />
                : <Send className="h-3.5 w-3.5" />}
            </button>
          </div>
          <p className="mt-1.5 text-center text-[10px] text-muted-foreground/60">
            ARIA usa datos reales · Enter para enviar
          </p>
        </div>
      </div>
    </>
  );
}

/* ─── Message bubble ───────────────────────────────────────────── */
function MessageBubble({ msg }: { msg: AriaMessage }) {
  const isUser = msg.role === "user";

  return (
    <div className={cn("flex gap-2 animate-in fade-in slide-in-from-bottom-2 duration-200", isUser ? "flex-row-reverse" : "flex-row")}>
      {!isUser && (
        <div className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-primary mt-1">
          <Bot className="h-3 w-3 text-primary-foreground" />
        </div>
      )}

      <div className={cn("flex max-w-[85%] flex-col gap-1", isUser && "items-end")}>
        {/* Tool calls */}
        {!isUser && msg.toolCalls && msg.toolCalls.length > 0 && (
          <div className="flex flex-col gap-1">
            {msg.toolCalls.map((tc, i) => (
              <div key={i} className="flex items-center gap-1.5 rounded-md bg-muted/60 px-2.5 py-1 text-[11px] text-muted-foreground">
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
              "rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed",
              isUser
                ? "bg-primary text-primary-foreground rounded-tr-sm"
                : msg.error
                ? "bg-destructive/10 text-destructive rounded-tl-sm"
                : "bg-muted text-foreground rounded-tl-sm"
            )}
          >
            {msg.error && (
              <div className="flex items-center gap-1.5 mb-1">
                <AlertCircle className="h-3.5 w-3.5" />
                <span className="text-xs font-medium">Error</span>
              </div>
            )}
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              components={{
                table: ({ children }) => (
                  <div className="overflow-x-auto my-2">
                    <table className="w-full text-xs border-collapse">{children}</table>
                  </div>
                ),
                thead: ({ children }) => (
                  <thead className="bg-muted-foreground/10">{children}</thead>
                ),
                th: ({ children }) => (
                  <th className="border border-border/50 px-2 py-1 text-left font-semibold">{children}</th>
                ),
                td: ({ children }) => (
                  <td className="border border-border/50 px-2 py-1">{children}</td>
                ),
                strong: ({ children }) => (
                  <strong className="font-semibold">{children}</strong>
                ),
                ul: ({ children }) => (
                  <ul className="list-disc list-inside space-y-0.5 my-1">{children}</ul>
                ),
                ol: ({ children }) => (
                  <ol className="list-decimal list-inside space-y-0.5 my-1">{children}</ol>
                ),
                p: ({ children }) => (
                  <p className="mb-1 last:mb-0">{children}</p>
                ),
              }}
            >
              {msg.text}
            </ReactMarkdown>
            {msg.streaming && !msg.text && (
              <span className="flex items-center gap-1.5 text-muted-foreground">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                <span className="text-xs">ARIA está pensando…</span>
              </span>
            )}
            {msg.streaming && msg.text && (
              <span className="ml-1 inline-block h-3.5 w-0.5 animate-pulse bg-current align-middle opacity-70" />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
