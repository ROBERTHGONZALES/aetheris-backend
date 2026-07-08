package com.aetheris.dao;

import com.aetheris.modelo.FlujodeAprobacion;
import com.aetheris.modelo.enums.EstadoFlujo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlujodeAprobacionDAO extends JpaRepository<FlujodeAprobacion, String> {

    Optional<FlujodeAprobacion> findByTransaccionId(String transaccionId);

    List<FlujodeAprobacion> findByEstado(EstadoFlujo estado);

    List<FlujodeAprobacion> findByEstadoOrderByFechaSolicitudAsc(EstadoFlujo estado);
}
