package com.aetheris.dao;

import com.aetheris.modelo.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAuditoriaDAO extends JpaRepository<LogAuditoria, String> {

    @Query("SELECT l FROM LogAuditoria l LEFT JOIN FETCH l.usuario ORDER BY l.fechaHora DESC")
    List<LogAuditoria> findAllByOrderByFechaHoraDesc();

    @Query("SELECT l FROM LogAuditoria l LEFT JOIN FETCH l.usuario WHERE l.usuario.id = :usuarioId ORDER BY l.fechaHora DESC")
    List<LogAuditoria> findByUsuarioIdOrderByFechaHoraDesc(@Param("usuarioId") String usuarioId);

    @Query("SELECT l FROM LogAuditoria l LEFT JOIN FETCH l.usuario WHERE l.modulo = :modulo ORDER BY l.fechaHora DESC")
    List<LogAuditoria> findByModuloOrderByFechaHoraDesc(@Param("modulo") String modulo);

    @Query("SELECT l FROM LogAuditoria l LEFT JOIN FETCH l.usuario WHERE l.fechaHora BETWEEN :inicio AND :fin ORDER BY l.fechaHora DESC")
    List<LogAuditoria> findByFechaHoraBetweenOrderByFechaHoraDesc(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT l FROM LogAuditoria l LEFT JOIN FETCH l.usuario WHERE LOWER(l.accion) LIKE LOWER(CONCAT('%', :accion, '%')) ORDER BY l.fechaHora DESC")
    List<LogAuditoria> findByAccionContainingIgnoreCase(@Param("accion") String accion);
}
