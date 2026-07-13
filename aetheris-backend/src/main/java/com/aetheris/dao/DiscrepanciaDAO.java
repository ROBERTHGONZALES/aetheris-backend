package com.aetheris.dao;

import com.aetheris.modelo.Discrepancia;
import com.aetheris.modelo.enums.EstadoDiscrepancia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscrepanciaDAO extends JpaRepository<Discrepancia, String> {

    List<Discrepancia> findByMovimientoBancarioId(String movimientoId);

    List<Discrepancia> findByEstado(EstadoDiscrepancia estado);
}
