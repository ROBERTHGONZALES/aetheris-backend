package com.aetheris.dao;

import com.aetheris.modelo.Reporte;
import com.aetheris.modelo.enums.TipoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteDAO extends JpaRepository<Reporte, String> {

    List<Reporte> findByUsuarioIdOrderByFechaGeneracionDesc(String usuarioId);

    List<Reporte> findByTipo(TipoReporte tipo);

    List<Reporte> findByPeriodo(String periodo);
}
