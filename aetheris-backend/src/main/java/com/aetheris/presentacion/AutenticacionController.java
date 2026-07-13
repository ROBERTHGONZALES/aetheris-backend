package com.aetheris.presentacion;

import com.aetheris.modelo.Sesion;
import com.aetheris.servicio.AutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@Valid @RequestBody LoginRequest req,
                                            HttpServletRequest httpReq) {
        String ip = httpReq.getRemoteAddr();
        try {
            Sesion sesion = autenticacionService.iniciarSesion(req.getCorreo(), req.getPassword(), ip);
            return ResponseEntity.ok(Map.of(
                "token",    sesion.getToken(),
                "sesionId", sesion.getId(),
                "usuario",  sesion.getUsuario().getNombreCompleto(),
                "rol",      sesion.getUsuario().getRol().getNombre()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/auth/logout */
    @PostMapping("/logout")
    public ResponseEntity<?> cerrarSesion(@RequestHeader("Authorization") String authHeader) {
        String token = extraerToken(authHeader);
        autenticacionService.cerrarSesion(token);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }

    /** GET /api/auth/validar */
    @GetMapping("/validar")
    public ResponseEntity<?> validarToken(@RequestHeader("Authorization") String authHeader) {
        boolean valido = autenticacionService.validarToken(extraerToken(authHeader));
        return valido
            ? ResponseEntity.ok(Map.of("valido", true))
            : ResponseEntity.status(401).body(Map.of("valido", false));
    }

    private String extraerToken(String header) {
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }

    @Data
    static class LoginRequest {
        @Email @NotBlank private String correo;
        @NotBlank private String password;
    }
}
