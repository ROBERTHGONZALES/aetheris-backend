package com.aetheris.presentacion;

import com.aetheris.dto.AriaChatRequest;
import com.aetheris.modelo.Usuario;
import com.aetheris.servicio.AriaService;
import com.aetheris.servicio.AutenticacionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/aria")
@RequiredArgsConstructor
public class AriaController {

    private final AriaService ariaService;
    private final AutenticacionService autenticacionService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** POST /api/aria/chat — respuesta en streaming SSE (text/event-stream). */
    @PostMapping(value = "/chat", produces = "text/event-stream")
    public SseEmitter chat(@RequestBody AriaChatRequest req,
                            @RequestHeader("Authorization") String auth,
                            HttpServletResponse response) {
        // Evita buffering en proxies intermedios (Railway/Nginx/Cloudflare)
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-store, no-transform");
        response.setHeader("X-Content-Type-Options", "nosniff");
        // Header de diagnóstico: confirma que este build está desplegado
        response.setHeader("X-Aria-Build", "streaming-v3");

        Usuario usuario = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.submit(() -> ariaService.chat(req.getMessage(), req.getHistory(), usuario, emitter));
        return emitter;
    }

    private String extraerToken(String header) {
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }
}
