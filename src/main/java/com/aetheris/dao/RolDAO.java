package com.aetheris.dao;

import com.aetheris.modelo.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolDAO extends JpaRepository<Rol, String> {

    Optional<Rol> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
