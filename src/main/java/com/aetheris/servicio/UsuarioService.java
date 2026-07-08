package com.aetheris.servicio;

import com.aetheris.dao.RolDAO;
import com.aetheris.dao.UsuarioDAO;
import com.aetheris.modelo.Rol;
import com.aetheris.modelo.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario crearUsuario(Usuario usuario, String passwordPlano, Usuario operador) {
        if (usuarioDAO.existsByCorreoElectronico(usuario.getCorreoElectronico())) {
            throw new RuntimeException("Ya existe un usuario con el correo: "
                    + usuario.getCorreoElectronico());
        }
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        Usuario saved = usuarioDAO.save(usuario);
        auditoriaService.registrar(operador, "CREAR_USUARIO", "ADMINISTRACION",
                "USUARIO", null, "usuario", saved.getId(), null);
        return saved;
    }

    @Transactional
    public void asignarRol(String idUsuario, String idRol, Usuario operador) {
        Usuario usuario = usuarioDAO.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));
        Rol rol = rolDAO.findById(idRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + idRol));
        usuario.setRol(rol);
        usuarioDAO.save(usuario);
        auditoriaService.registrar(operador, "ASIGNAR_ROL", "ADMINISTRACION",
                "USUARIO", null, "usuario", idUsuario, null);
    }

    @Transactional
    public void activarUsuario(String id, Usuario operador) {
        cambiarEstado(id, true, operador, "ACTIVAR_USUARIO");
    }

    @Transactional
    public void desactivarUsuario(String id, Usuario operador) {
        cambiarEstado(id, false, operador, "DESACTIVAR_USUARIO");
    }

    private void cambiarEstado(String id, boolean estado, Usuario operador, String accion) {
        Usuario usuario = usuarioDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        usuario.setEstado(estado);
        usuarioDAO.save(usuario);
        auditoriaService.registrar(operador, accion, "ADMINISTRACION",
                "USUARIO", null, "usuario", id, null);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuario(String id) {
        return usuarioDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuariosPorRol(String idRol) {
        Rol rol = rolDAO.findById(idRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + idRol));
        return usuarioDAO.findByRol(rol);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioDAO.findAll();
    }
}
