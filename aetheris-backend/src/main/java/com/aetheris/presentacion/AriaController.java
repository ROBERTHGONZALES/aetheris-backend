package com.aetheris.presentacion;

import com.aetheris.dto.AriaChatRequest;
import com.aetheris.modelo.Usuario;
import com.aetheris.servicio.AriaService;
import com.aetheris.servicio.AutenticacionService;
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

    // La llamada a Gemini + herramientas es bloqueante; se ejecuta en un hilo
    // aparte para no bloquear el hilo de la petición mientras se transmite el SSE.
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** POST /api/aria/chat — respuesta en streaming SSE (text/event-stream). */
    @PostMapping(value = "/chat", produces = "text/event-stream")
    public SseEmitter chat(@RequestBody AriaChatRequest req,
                            @RequestHeader("Authorization") String auth) {
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
