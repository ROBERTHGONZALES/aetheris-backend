package com.aetheris.presentacion;

import com.aetheris.modelo.PartidaPresupuestaria;
import com.aetheris.modelo.Usuario;
import com.aetheris.servicio.AutenticacionService;
import com.aetheris.servicio.PresupuestoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/presupuesto")
@RequiredArgsConstructor
public class PresupuestoController {

    private final PresupuestoService presupuestoService;
    private final AutenticacionService autenticacionService;

    /** POST /api/presupuesto — solo quien administra el presupuesto (ADMIN, CONTADOR). */
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @PostMapping
    public ResponseEntity<PartidaPresupuestaria> crear(
            @RequestBody PartidaPresupuestaria partida,
            @RequestHeader("Authorization") String auth) {
        Usuario usuario = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(presupuestoService.crearPartida(partida, usuario));
    }

    /** GET /api/presupuesto?sedeId=xxx */
    @GetMapping
    public ResponseEntity<List<PartidaPresupuestaria>> listarPorSede(
            @RequestParam String sedeId) {
        return ResponseEntity.ok(presupuestoService.listarPorSede(sedeId));
    }

    /** GET /api/presupuesto/alerta */
    @GetMapping("/alerta")
    public ResponseEntity<List<PartidaPresupuestaria>> listarEnAlerta() {
        return ResponseEntity.ok(presupuestoService.listarPartidasEnAlerta());
    }

    /** GET /api/presupuesto/saldo?idPartida=xxx */
    @GetMapping("/saldo")
    public ResponseEntity<?> calcularSaldo(@RequestParam String idPartida) {
        return ResponseEntity.ok(presupuestoService.calcularSaldoDisponible(idPartida));
    }

    private String extraerToken(String header) {
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }
}
