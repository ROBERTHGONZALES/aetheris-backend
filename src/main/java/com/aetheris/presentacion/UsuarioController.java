package com.aetheris.presentacion;

import com.aetheris.modelo.Usuario;
import com.aetheris.servicio.AutenticacionService;
import com.aetheris.servicio.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Gestión de usuarios y roles: exclusivo de ADMIN. */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AutenticacionService autenticacionService;

    /** POST /api/usuarios */
    @PostMapping
    public ResponseEntity<Usuario> crear(
            @Valid @RequestBody NuevoUsuarioRequest req,
            @RequestHeader("Authorization") String auth) {
        Usuario operador = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        Usuario usuario = Usuario.builder()
                .nombreCompleto(req.getNombreCompleto())
                .correoElectronico(req.getCorreoElectronico())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.crearUsuario(usuario, req.getPassword(), operador));
    }

    /** PUT /api/usuarios/{id}/rol */
    @PutMapping("/{id}/rol")
    public ResponseEntity<?> asignarRol(@PathVariable String id,
                                         @RequestParam String rolId,
                                         @RequestHeader("Authorization") String auth) {
        Usuario operador = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        usuarioService.asignarRol(id, rolId, operador);
        return ResponseEntity.ok(Map.of("mensaje", "Rol asignado correctamente"));
    }

    /** PUT /api/usuarios/{id}/activar */
    @PutMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable String id,
                                      @RequestHeader("Authorization") String auth) {
        Usuario operador = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        usuarioService.activarUsuario(id, operador);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario activado"));
    }

    /** PUT /api/usuarios/{id}/desactivar */
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id,
                                         @RequestHeader("Authorization") String auth) {
        Usuario operador = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        usuarioService.desactivarUsuario(id, operador);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario desactivado"));
    }

    /** GET /api/usuarios?rolId=xxx (rolId es opcional: sin filtro lista todos) */
    @GetMapping
    public ResponseEntity<List<Usuario>> listarPorRol(@RequestParam(required = false) String rolId) {
        if (rolId == null || rolId.isBlank()) {
            return ResponseEntity.ok(usuarioService.listarTodos());
        }
        return ResponseEntity.ok(usuarioService.listarUsuariosPorRol(rolId));
    }

    /** GET /api/usuarios/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtener(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuario(id));
    }

    private String extraerToken(String h) {
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }

    @Data
    static class NuevoUsuarioRequest {
        @NotBlank private String nombreCompleto;
        @NotBlank private String correoElectronico;
        @NotBlank private String password;
    }
}
