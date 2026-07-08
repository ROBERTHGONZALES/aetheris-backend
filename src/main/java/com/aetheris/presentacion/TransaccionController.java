package com.aetheris.presentacion;

import com.aetheris.modelo.TransaccionFinanciera;
import com.aetheris.modelo.Usuario;
import com.aetheris.servicio.AutenticacionService;
import com.aetheris.servicio.TransaccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;
    private final AutenticacionService autenticacionService;

    /** POST /api/transacciones */
    @PostMapping
    public ResponseEntity<TransaccionFinanciera> registrar(
            @RequestBody TransaccionFinanciera tx,
            @RequestHeader("Authorization") String auth) {
        Usuario usuario = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transaccionService.registrarTransaccion(tx, usuario));
    }

    /** GET /api/transacciones/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<TransaccionFinanciera> obtener(@PathVariable String id) {
        return ResponseEntity.ok(transaccionService.obtenerTransaccion(id));
    }

    /** GET /api/transacciones?sedeId=xxx */
    @GetMapping
    public ResponseEntity<List<TransaccionFinanciera>> listarPorSede(
            @RequestParam String sedeId) {
        return ResponseEntity.ok(transaccionService.listarTransaccionesPorSede(sedeId));
    }

    /** GET /api/transacciones/pendientes */
    @GetMapping("/pendientes")
    public ResponseEntity<List<TransaccionFinanciera>> listarPendientes() {
        return ResponseEntity.ok(transaccionService.listarTransaccionesPendientes());
    }

    /** GET /api/transacciones/periodo?inicio=2025-01-01&fin=2025-01-31 */
    @GetMapping("/periodo")
    public ResponseEntity<List<TransaccionFinanciera>> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(transaccionService.listarTransaccionesPorPeriodo(inicio, fin));
    }

    private String extraerToken(String header) {
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }
}
