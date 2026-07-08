package com.aetheris.dao;

import com.aetheris.modelo.TransaccionFinanciera;
import com.aetheris.modelo.enums.EstadoAprobacion;
import com.aetheris.modelo.enums.TipoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransaccionFinancieraDAO extends JpaRepository<TransaccionFinanciera, String> {

    List<TransaccionFinanciera> findBySedeId(String sedeId);

    List<TransaccionFinanciera> findByEstadoAprobacion(EstadoAprobacion estado);

    List<TransaccionFinanciera> findByFechaBetweenOrderByFechaDesc(
            LocalDate inicio, LocalDate fin);

    List<TransaccionFinanciera> findByCategoriaId(String categoriaId);

    List<TransaccionFinanciera> findBySedeIdAndEstadoAprobacion(
            String sedeId, EstadoAprobacion estado);

    List<TransaccionFinanciera> findByTipo(TipoTransaccion tipo);
}
