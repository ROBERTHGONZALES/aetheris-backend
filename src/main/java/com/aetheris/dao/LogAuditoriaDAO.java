package com.aetheris.dao;

import com.aetheris.modelo.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAuditoriaDAO extends JpaRepository<LogAuditoria, String> {

    List<LogAuditoria> findByUsuarioIdOrderByFechaHoraDesc(String usuarioId);

    List<LogAuditoria> findByModuloOrderByFechaHoraDesc(String modulo);

    List<LogAuditoria> findByFechaHoraBetweenOrderByFechaHoraDesc(
            LocalDateTime inicio, LocalDateTime fin);

    List<LogAuditoria> findByAccionContainingIgnoreCase(String accion);

    List<LogAuditoria> findAllByOrderByFechaHoraDesc();
}
