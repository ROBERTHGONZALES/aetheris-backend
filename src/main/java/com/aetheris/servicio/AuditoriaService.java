package com.aetheris.servicio;

import com.aetheris.dao.LogAuditoriaDAO;
import com.aetheris.modelo.LogAuditoria;
import com.aetheris.modelo.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio transversal de auditoría.
 * Todos los demás servicios lo usan para registrar eventos de forma inmutable.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final LogAuditoriaDAO logAuditoriaDAO;

    @Transactional
    public void registrar(Usuario usuario, String accion, String modulo,
                          String categoria, String ip) {
        registrar(usuario, accion, modulo, categoria, ip, null, null, null);
    }

    @Transactional
    public void registrar(Usuario usuario, String accion, String modulo,
                          String categoria, String ip,
                          String entidadAfectada, String entidadId, String detalle) {
        LogAuditoria log = LogAuditoria.builder()
                .usuario(usuario)
                .accion(accion)
                .modulo(modulo)
                .categoria(categoria)
                .direccionIp(ip)
                .entidadAfectada(entidadAfectada)
                .entidadId(entidadId)
                .detalle(detalle)
                .fechaHora(LocalDateTime.now())
                .build();
        logAuditoriaDAO.save(log);
    }

    @Transactional(readOnly = true)
    public List<LogAuditoria> listarPorUsuario(String usuarioId) {
        return logAuditoriaDAO.findByUsuarioIdOrderByFechaHoraDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<LogAuditoria> listarPorModulo(String modulo) {
        return logAuditoriaDAO.findByModuloOrderByFechaHoraDesc(modulo);
    }

    @Transactional(readOnly = true)
    public List<LogAuditoria> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        return logAuditoriaDAO.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);
    }
}
