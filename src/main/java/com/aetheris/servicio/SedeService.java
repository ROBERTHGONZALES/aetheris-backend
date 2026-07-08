package com.aetheris.servicio;

import com.aetheris.dao.SedeDAO;
import com.aetheris.modelo.Sede;
import com.aetheris.modelo.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeService {

    private final SedeDAO sedeDAO;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Sede crearSede(Sede sede, Usuario operador) {
        if (sedeDAO.existsByCodigo(sede.getCodigo())) {
            throw new RuntimeException("Ya existe una sede con el código: " + sede.getCodigo());
        }
        Sede saved = sedeDAO.save(sede);
        auditoriaService.registrar(operador, "CREAR_SEDE", "ADMINISTRACION",
                "SEDE", null, "sede", saved.getId(), null);
        return saved;
    }

    @Transactional
    public Sede actualizarSede(Sede sede, Usuario operador) {
        Sede existing = sedeDAO.findById(sede.getId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada: " + sede.getId()));
        existing.setNombre(sede.getNombre());
        existing.setPais(sede.getPais());
        existing.setMoneda(sede.getMoneda());
        Sede saved = sedeDAO.save(existing);
        auditoriaService.registrar(operador, "ACTUALIZAR_SEDE", "ADMINISTRACION",
                "SEDE", null, "sede", saved.getId(), null);
        return saved;
    }

    @Transactional
    public void configurarLimiteAprobacion(String idSede, BigDecimal monto, Usuario operador) {
        Sede sede = sedeDAO.findById(idSede)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada: " + idSede));
        sede.setMontoLimiteAprobacion(monto);
        sedeDAO.save(sede);
        auditoriaService.registrar(operador, "CONFIGURAR_LIMITE_APROBACION",
                "ADMINISTRACION", "SEDE", null, "sede", idSede, null);
    }

    @Transactional(readOnly = true)
    public Sede obtenerSede(String id) {
        return sedeDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<Sede> listarSedesActivas() {
        return sedeDAO.findByEstadoTrue();
    }

    @Transactional(readOnly = true)
    public List<Sede> listarTodas() {
        return sedeDAO.findAll();
    }
}
