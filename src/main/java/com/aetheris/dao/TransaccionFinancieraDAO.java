package com.aetheris.dao;

import com.aetheris.modelo.TransaccionFinanciera;
import com.aetheris.modelo.enums.EstadoAprobacion;
import com.aetheris.modelo.enums.TipoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    List<TransaccionFinanciera> findAllByOrderByFechaDesc();

    // ── Consultas analíticas agregadas ─────────────────────────────────────

    /**
     * Resumen de ingresos/egresos agrupado por año y mes.
     * Columnas: [0]=año (int), [1]=mes (int), [2]=tipo (String),
     *           [3]=total (BigDecimal), [4]=cantidad (Long)
     */
    @Query("SELECT YEAR(t.fecha), MONTH(t.fecha), t.tipo, SUM(t.monto), COUNT(t) " +
           "FROM TransaccionFinanciera t " +
           "GROUP BY YEAR(t.fecha), MONTH(t.fecha), t.tipo " +
           "ORDER BY YEAR(t.fecha) DESC, MONTH(t.fecha) DESC")
    List<Object[]> resumenMensual();

    /**
     * Totales de ingresos/egresos agrupados por sede.
     * Columnas: [0]=nombreSede (String), [1]=tipo (String),
     *           [2]=total (BigDecimal), [3]=cantidad (Long)
     */
    @Query("SELECT t.sede.nombre, t.tipo, SUM(t.monto), COUNT(t) " +
           "FROM TransaccionFinanciera t " +
           "GROUP BY t.sede.nombre, t.tipo " +
           "ORDER BY t.sede.nombre ASC")
    List<Object[]> resumenPorSede();

    /**
     * Totales de ingresos/egresos agrupados por categoría contable.
     * Columnas: [0]=nombreCategoria (String), [1]=tipo (String),
     *           [2]=total (BigDecimal), [3]=cantidad (Long)
     */
    @Query("SELECT t.categoria.nombre, t.tipo, SUM(t.monto), COUNT(t) " +
           "FROM TransaccionFinanciera t " +
           "GROUP BY t.categoria.nombre, t.tipo " +
           "ORDER BY SUM(t.monto) DESC")
    List<Object[]> resumenPorCategoria();
}
