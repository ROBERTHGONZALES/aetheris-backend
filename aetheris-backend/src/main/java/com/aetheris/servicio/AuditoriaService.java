package com.aetheris.servicio;

import com.aetheris.dao.LogAuditoriaDAO;
import com.aetheris.dto.LogAuditoriaDTO;
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
 *
 * Los métodos de consulta devuelven LogAuditoriaDTO en lugar de la entidad JPA
 * para evitar el problema de usuario=null causado por el proxy Hibernate lazy:
 * con spring.jpa.open-in-view=false el proxy no se inicializa al serializar,
 * y Hibernate6Module lo convierte silenciosamente a null. El mapeo a DTO ocurre
 * dentro de la transacción, cuando el usuario ya está cargado por JOIN FETCH.
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
    public List<LogAuditoriaDTO> listarPorUsuario(String usuarioId) {
        List<LogAuditoria> logs = (usuarioId == null || usuarioId.isBlank())
                ? logAuditoriaDAO.findAllByOrderByFechaHoraDesc()
                : logAuditoriaDAO.findByUsuarioIdOrderByFechaHoraDesc(usuarioId);
        return logs.stream().map(LogAuditoriaDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LogAuditoriaDTO> listarPorModulo(String modulo) {
        return logAuditoriaDAO.findByModuloOrderByFechaHoraDesc(modulo)
                .stream().map(LogAuditoriaDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LogAuditoriaDTO> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        return logAuditoriaDAO.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin)
                .stream().map(LogAuditoriaDTO::from).toList();
    }
}
