package com.aetheris.presentacion;

import com.aetheris.dao.RolDAO;
import com.aetheris.modelo.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de roles disponibles (ADMIN, CONTADOR, APROBADOR, AUDITOR).
 * Exclusivo de ADMIN: se usa para poblar el selector de roles al gestionar
 * usuarios desde la pantalla de administración.
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RolController {

    private final RolDAO rolDAO;

    /** GET /api/roles */
    @GetMapping
    public ResponseEntity<List<Rol>> listar() {
        return ResponseEntity.ok(rolDAO.findAll());
    }
}
