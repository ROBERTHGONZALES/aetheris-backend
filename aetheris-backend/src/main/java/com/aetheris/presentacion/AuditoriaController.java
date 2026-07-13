package com.aetheris.presentacion;

import com.aetheris.dto.LogAuditoriaDTO;
import com.aetheris.servicio.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    /** GET /api/auditoria?usuarioId=xxx (usuarioId es opcional: sin filtro lista todo) */
    @GetMapping
    public ResponseEntity<List<LogAuditoriaDTO>> listarPorUsuario(
            @RequestParam(required = false) String usuarioId) {
        return ResponseEntity.ok(auditoriaService.listarPorUsuario(usuarioId));
    }

    /** GET /api/auditoria/modulo?modulo=TRANSACCIONES */
    @GetMapping("/modulo")
    public ResponseEntity<List<LogAuditoriaDTO>> listarPorModulo(
            @RequestParam String modulo) {
        return ResponseEntity.ok(auditoriaService.listarPorModulo(modulo));
    }

    /** GET /api/auditoria/periodo?inicio=...&fin=... */
    @GetMapping("/periodo")
    public ResponseEntity<List<LogAuditoriaDTO>> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(auditoriaService.listarPorPeriodo(inicio, fin));
    }
}
