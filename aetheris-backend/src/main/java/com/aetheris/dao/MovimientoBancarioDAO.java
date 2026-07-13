package com.aetheris.dao;

import com.aetheris.modelo.MovimientoBancario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoBancarioDAO extends JpaRepository<MovimientoBancario, String> {

    List<MovimientoBancario> findByConciliacionId(String conciliacionId);

    List<MovimientoBancario> findByConciliacionIdAndEstadoConciliacionFalse(
            String conciliacionId);
}
