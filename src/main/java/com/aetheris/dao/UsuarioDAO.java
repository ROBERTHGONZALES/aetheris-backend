package com.aetheris.dao;

import com.aetheris.modelo.Rol;
import com.aetheris.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioDAO extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByCorreoElectronico(String correo);

    List<Usuario> findByRol(Rol rol);

    List<Usuario> findByEstadoTrue();

    boolean existsByCorreoElectronico(String correo);
}
