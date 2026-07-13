package com.aetheris.presentacion;

import com.aetheris.modelo.FlujodeAprobacion;
import com.aetheris.modelo.Usuario;
import com.aetheris.servicio.AprobacionService;
import com.aetheris.servicio.AutenticacionService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Bandeja de aprobaciones: solo ADMIN y APROBADOR pueden ver/resolver. */
@RestController
@RequestMapping("/aprobaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','APROBADOR')")
public class AprobacionController {

    private final AprobacionService aprobacionService;
    private final AutenticacionService autenticacionService;

    /** GET /api/aprobaciones/pendientes */
    @GetMapping("/pendientes")
    public ResponseEntity<List<FlujodeAprobacion>> listarPendientes() {
        return ResponseEntity.ok(aprobacionService.listarFlujosPendientes());
    }

    /** PUT /api/aprobaciones/{id}/aprobar */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable String id,
                                      @RequestBody ResolucionRequest req,
                                      @RequestHeader("Authorization") String auth) {
        Usuario cfo = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        try {
            aprobacionService.aprobarTransaccion(id, cfo, req.getObservacion());
            return ResponseEntity.ok(Map.of("mensaje", "Transacción aprobada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/aprobaciones/{id}/rechazar */
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable String id,
                                       @RequestBody ResolucionRequest req,
                                       @RequestHeader("Authorization") String auth) {
        Usuario cfo = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        try {
            aprobacionService.rechazarTransaccion(id, cfo, req.getObservacion());
            return ResponseEntity.ok(Map.of("mensaje", "Transacción rechazada"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String extraerToken(String header) {
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }

    @Data
    static class ResolucionRequest {
        @NotBlank private String observacion;
    }
}
