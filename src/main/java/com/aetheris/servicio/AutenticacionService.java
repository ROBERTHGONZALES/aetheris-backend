package com.aetheris.servicio;

import com.aetheris.dao.SesionDAO;
import com.aetheris.dao.UsuarioDAO;
import com.aetheris.modelo.Sesion;
import com.aetheris.modelo.Usuario;
import com.aetheris.modelo.enums.EstadoSesion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final UsuarioDAO usuarioDAO;
    private final SesionDAO sesionDAO;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${aetheris.sesion.inactividad-minutos:30}")
    private int minutosInactividad;

    @Transactional
    public Sesion iniciarSesion(String correo, String password, String ip) {
        Usuario usuario = usuarioDAO.findByCorreoElectronico(correo)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new RuntimeException("Cuenta desactivada");
        }
        if (usuario.getBloqueadoHasta() != null &&
                usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Cuenta bloqueada temporalmente");
        }
        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            if (usuario.getIntentosFallidos() >= 5) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(30));
            }
            usuarioDAO.save(usuario);
            throw new RuntimeException("Credenciales inválidas");
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoLogin(LocalDateTime.now());
        usuarioDAO.save(usuario);

        String token = jwtService.generarToken(usuario);
        Sesion sesion = Sesion.builder()
                .usuario(usuario)
                .token(token)
                .fechaInicio(LocalDateTime.now())
                .ipOrigen(ip)
                .estadoSesion(EstadoSesion.ACTIVA)
                .build();
        sesionDAO.save(sesion);

        auditoriaService.registrar(usuario, "INICIO_SESION", "AUTENTICACION",
                "SESION", ip, "sesion", sesion.getId(), null);
        return sesion;
    }

    @Transactional
    public void cerrarSesion(String token) {
        sesionDAO.findByToken(token).ifPresent(sesion -> {
            sesion.setEstadoSesion(EstadoSesion.CERRADA);
            sesion.setFechaCierre(LocalDateTime.now());
            sesionDAO.save(sesion);
            auditoriaService.registrar(sesion.getUsuario(), "CIERRE_SESION",
                    "AUTENTICACION", "SESION", null);
        });
    }

    @Transactional(readOnly = true)
    public boolean validarToken(String token) {
        return sesionDAO.findByToken(token)
                .map(s -> s.isActiva() && jwtService.validarToken(token))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorToken(String token) {
        return sesionDAO.findByToken(token)
                .filter(Sesion::isActiva)
                .map(Sesion::getUsuario)
                .orElseThrow(() -> new RuntimeException("Token inválido o expirado"));
    }

    @Transactional
    public void cerrarSesionesPorInactividad() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosInactividad);
        int cerradas = sesionDAO.cerrarSesionesPorInactividad(limite, LocalDateTime.now());
        if (cerradas > 0) {
            auditoriaService.registrar(null, "CIERRE_SESION_INACTIVIDAD",
                    "AUTENTICACION", "SESION", null, null, null,
                    "{\"cantidad\": " + cerradas + "}");
        }
    }

    @Transactional(readOnly = true)
    public boolean verificarPermiso(Usuario usuario, String modulo) {
        return usuario.getRol().tienePermiso(modulo);
    }

    @Transactional(readOnly = true)
    public List<Sesion> obtenerSesionesActivas(String usuarioId) {
        return sesionDAO.findByUsuarioIdAndEstadoSesion(usuarioId, EstadoSesion.ACTIVA);
    }
}
