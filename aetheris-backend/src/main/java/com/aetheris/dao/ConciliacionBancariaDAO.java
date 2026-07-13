package com.aetheris.dao;

import com.aetheris.modelo.ConciliacionBancaria;
import com.aetheris.modelo.enums.EstadoConciliacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConciliacionBancariaDAO extends JpaRepository<ConciliacionBancaria, String> {

    List<ConciliacionBancaria> findByCuentaBancariaId(String cuentaId);

    List<ConciliacionBancaria> findByPeriodo(String periodo);

    List<ConciliacionBancaria> findByEstado(EstadoConciliacion estado);

    List<ConciliacionBancaria> findByCuentaBancariaIdAndPeriodo(
            String cuentaId, String periodo);
}
