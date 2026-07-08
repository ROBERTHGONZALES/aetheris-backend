package com.aetheris.dao;

import com.aetheris.modelo.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SedeDAO extends JpaRepository<Sede, String> {

    Optional<Sede> findByCodigo(String codigo);

    List<Sede> findByEstadoTrue();

    boolean existsByCodigo(String codigo);
}
