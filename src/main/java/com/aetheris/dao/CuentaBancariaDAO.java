package com.aetheris.dao;

import com.aetheris.modelo.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuentaBancariaDAO extends JpaRepository<CuentaBancaria, String> {

    List<CuentaBancaria> findBySedeId(String sedeId);

    List<CuentaBancaria> findByEstadoTrue();
}
