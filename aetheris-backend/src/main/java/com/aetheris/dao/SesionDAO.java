package com.aetheris.dao;

import com.aetheris.modelo.Sesion;
import com.aetheris.modelo.enums.EstadoSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionDAO extends JpaRepository<Sesion, String> {

    Optional<Sesion> findByToken(String token);

    List<Sesion> findByUsuarioIdAndEstadoSesion(String usuarioId, EstadoSesion estado);

    @Modifying
    @Query("UPDATE Sesion s SET s.estadoSesion = 'CERRADA', s.fechaCierre = :ahora " +
           "WHERE s.estadoSesion = 'ACTIVA' AND s.fechaInicio < :limite")
    int cerrarSesionesPorInactividad(@Param("limite") LocalDateTime limite,
                                     @Param("ahora") LocalDateTime ahora);
}
