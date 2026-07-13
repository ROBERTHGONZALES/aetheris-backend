package com.aetheris.presentacion;

import com.aetheris.modelo.ConciliacionBancaria;
import com.aetheris.modelo.Discrepancia;
import com.aetheris.modelo.MovimientoBancario;
import com.aetheris.modelo.Usuario;
import com.aetheris.modelo.enums.TipoResolucionDisc;
import com.aetheris.servicio.AutenticacionService;
import com.aetheris.servicio.ConciliacionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Conciliación bancaria: gestionada por ADMIN/CONTADOR; AUDITOR solo lectura. */
@RestController
@RequestMapping("/conciliacion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','CONTADOR','AUDITOR')")
public class ConciliacionController {

    private final ConciliacionService conciliacionService;
    private final AutenticacionService autenticacionService;

    /** POST /api/conciliacion */
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @PostMapping
    public ResponseEntity<ConciliacionBancaria> iniciar(
            @RequestParam String cuentaId,
            @RequestParam String periodo,
            @RequestHeader("Authorization") String auth) {
        Usuario u = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conciliacionService.iniciarConciliacion(cuentaId, periodo, u));
    }

    /** POST /api/conciliacion/{id}/movimientos */
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @PostMapping("/{id}/movimientos")
    public ResponseEntity<List<MovimientoBancario>> importarMovimientos(
            @PathVariable String id,
            @RequestBody List<MovimientoBancario> movimientos) {
        return ResponseEntity.ok(conciliacionService.importarMovimientos(id, movimientos));
    }

    /** POST /api/conciliacion/{id}/cruce */
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @PostMapping("/{id}/cruce")
    public ResponseEntity<?> ejecutarCruce(@PathVariable String id) {
        conciliacionService.ejecutarCruce(id);
        return ResponseEntity.ok(Map.of("mensaje", "Cruce automático ejecutado correctamente"));
    }

    /** GET /api/conciliacion/{id}/discrepancias */
    @GetMapping("/{id}/discrepancias")
    public ResponseEntity<List<Discrepancia>> listarDiscrepancias(@PathVariable String id) {
        return ResponseEntity.ok(conciliacionService.listarDiscrepancias(id));
    }

    /** PUT /api/conciliacion/discrepancias/{discId}/resolver */
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @PutMapping("/discrepancias/{discId}/resolver")
    public ResponseEntity<?> resolverDiscrepancia(
            @PathVariable String discId,
            @RequestBody ResolucionDiscRequest req,
            @RequestHeader("Authorization") String auth) {
        Usuario u = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        conciliacionService.resolverDiscrepancia(discId, req.getTipo(), req.getJustificacion(), u);
        return ResponseEntity.ok(Map.of("mensaje", "Discrepancia resuelta correctamente"));
    }

    private String extraerToken(String h) {
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }

    @Data
    static class ResolucionDiscRequest {
        private TipoResolucionDisc tipo;
        private String justificacion;
    }
}
