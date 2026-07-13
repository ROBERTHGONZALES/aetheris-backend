package com.aetheris.dao;

import com.aetheris.modelo.PartidaPresupuestaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartidaPresupuestariaDAO extends JpaRepository<PartidaPresupuestaria, String> {

    Optional<PartidaPresupuestaria> findBySedeIdAndCategoriaIdAndPeriodo(
            String sedeId, String categoriaId, String periodo);

    List<PartidaPresupuestaria> findBySedeId(String sedeId);

    List<PartidaPresupuestaria> findByPeriodo(String periodo);

    @Query("SELECT p FROM PartidaPresupuestaria p WHERE " +
           "p.montoPresupuestado > 0 AND " +
           "(p.montoEjecutado / p.montoPresupuestado) >= :umbral")
    List<PartidaPresupuestaria> findEnAlerta(@Param("umbral") BigDecimal umbral);
}
