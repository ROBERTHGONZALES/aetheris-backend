package com.aetheris.dao;

import com.aetheris.modelo.FlujodeAprobacion;
import com.aetheris.modelo.enums.EstadoFlujo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlujodeAprobacionDAO extends JpaRepository<FlujodeAprobacion, String> {

    Optional<FlujodeAprobacion> findByTransaccionId(String transaccionId);

    List<FlujodeAprobacion> findByEstado(EstadoFlujo estado);

    List<FlujodeAprobacion> findByEstadoOrderByFechaSolicitudAsc(EstadoFlujo estado);

    /**
     * Same as findByEstadoOrderByFechaSolicitudAsc but eagerly fetches the
     * lazy "transaccion" (and its "sede") association so it can be safely
     * serialized to JSON after the transaction/session closes. Without the
     * JOIN FETCH, the controller throws:
     * "could not initialize proxy [...TransaccionFinanciera#...] - no Session".
     */
    @Query("SELECT f FROM FlujodeAprobacion f " +
            "JOIN FETCH f.transaccion t " +
            "JOIN FETCH t.sede " +
            "WHERE f.estado = :estado " +
            "ORDER BY f.fechaSolicitud ASC")
    List<FlujodeAprobacion> findPendientesConTransaccion(@Param("estado") EstadoFlujo estado);
}
