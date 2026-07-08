package com.aetheris.config;

import com.aetheris.dao.SesionDAO;
import com.aetheris.dao.UsuarioDAO;
import com.aetheris.modelo.Sesion;
import com.aetheris.modelo.Usuario;
import com.aetheris.modelo.enums.EstadoSesion;
import com.aetheris.servicio.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro JWT: extrae el token del header Authorization, lo valida contra
 * la tabla sesion (token activo en BD) y carga el usuario en el SecurityContext.
 * Registrado en SecurityConfig antes de UsernamePasswordAuthenticationFilter.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SesionDAO sesionDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 1. Validar firma y expiración del JWT
        if (!jwtService.validarToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Verificar que la sesión exista y esté activa en BD
        Optional<Sesion> sesionOpt = sesionDAO.findByToken(token);
        if (sesionOpt.isEmpty() || !EstadoSesion.ACTIVA.equals(sesionOpt.get().getEstadoSesion())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Cargar usuario y registrar en SecurityContext
        String usuarioId = jwtService.extraerUsuarioId(token);
        usuarioDAO.findById(usuarioId).ifPresent(usuario -> {
            if (Boolean.TRUE.equals(usuario.getEstado())
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        });

        filterChain.doFilter(request, response);
    }
}
