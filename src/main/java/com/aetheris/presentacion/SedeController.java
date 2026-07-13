package com.aetheris.presentacion;

import com.aetheris.modelo.Sede;
import com.aetheris.modelo.Usuario;
import com.aetheris.servicio.AutenticacionService;
import com.aetheris.servicio.SedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Gestión de sedes y límites de aprobación: exclusivo de ADMIN. */
@RestController
@RequestMapping("/sedes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SedeController {

    private final SedeService sedeService;
    private final AutenticacionService autenticacionService;

    /** POST /api/sedes */
    @PostMapping
    public ResponseEntity<Sede> crear(@RequestBody Sede sede,
                                       @RequestHeader("Authorization") String auth) {
        Usuario operador = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sedeService.crearSede(sede, operador));
    }

    /** PUT /api/sedes/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Sede> actualizar(@PathVariable String id,
                                            @RequestBody Sede sede,
                                            @RequestHeader("Authorization") String auth) {
        sede.setId(id);
        Usuario operador = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.ok(sedeService.actualizarSede(sede, operador));
    }

    /** PUT /api/sedes/{id}/limite */
    @PutMapping("/{id}/limite")
    public ResponseEntity<?> configurarLimite(@PathVariable String id,
                                               @RequestParam BigDecimal monto,
                                               @RequestHeader("Authorization") String auth) {
        Usuario operador = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        sedeService.configurarLimiteAprobacion(id, monto, operador);
        return ResponseEntity.ok(Map.of("mensaje", "Límite de aprobación actualizado"));
    }

    /** GET /api/sedes */
    @GetMapping
    public ResponseEntity<List<Sede>> listar() {
        return ResponseEntity.ok(sedeService.listarSedesActivas());
    }

    /** GET /api/sedes/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Sede> obtener(@PathVariable String id) {
        return ResponseEntity.ok(sedeService.obtenerSede(id));
    }

    private String extraerToken(String h) {
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }
}
