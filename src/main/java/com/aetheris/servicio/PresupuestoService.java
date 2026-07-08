package com.aetheris.servicio;

import com.aetheris.dao.PartidaPresupuestariaDAO;
import com.aetheris.modelo.PartidaPresupuestaria;
import com.aetheris.modelo.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresupuestoService {

    private final PartidaPresupuestariaDAO partidaDAO;
    private final AuditoriaService auditoriaService;

    @Value("${aetheris.presupuesto.umbral-alerta:0.90}")
    private BigDecimal umbralAlerta;

    @Transactional
    public PartidaPresupuestaria crearPartida(PartidaPresupuestaria partida, Usuario operador) {
        PartidaPresupuestaria saved = partidaDAO.save(partida);
        auditoriaService.registrar(operador, "CREAR_PARTIDA", "PRESUPUESTO",
                "PARTIDA_PRESUPUESTARIA", null, "partida_presupuestaria", saved.getId(), null);
        return saved;
    }

    @Transactional
    public void actualizarEjecucion(String idPartida, BigDecimal monto) {
        PartidaPresupuestaria partida = partidaDAO.findById(idPartida)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada: " + idPartida));
        partida.actualizarEjecucion(monto);
        partidaDAO.save(partida);
        if (partida.superaUmbralAlerta()) {
            generarAlertaPresupuestal(partida);
        }
    }

    @Transactional(readOnly = true)
    public PartidaPresupuestaria obtenerPartida(String sedeId, String catId, String periodo) {
        return partidaDAO.findBySedeIdAndCategoriaIdAndPeriodo(sedeId, catId, periodo)
                .orElseThrow(() -> new RuntimeException(
                        "Partida no encontrada para sede=" + sedeId
                        + " cat=" + catId + " periodo=" + periodo));
    }

    @Transactional(readOnly = true)
    public List<PartidaPresupuestaria> listarPartidasEnAlerta() {
        return partidaDAO.findEnAlerta(umbralAlerta);
    }

    @Transactional(readOnly = true)
    public List<PartidaPresupuestaria> listarPorSede(String sedeId) {
        return partidaDAO.findBySedeId(sedeId);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularSaldoDisponible(String idPartida) {
        return partidaDAO.findById(idPartida)
                .map(PartidaPresupuestaria::calcularSaldoDisponible)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada: " + idPartida));
    }

    private void generarAlertaPresupuestal(PartidaPresupuestaria partida) {
        // Aquí se puede integrar con NotificacionService o mensajería
        auditoriaService.registrar(null, "ALERTA_PRESUPUESTO", "PRESUPUESTO",
                "PARTIDA_PRESUPUESTARIA", null, "partida_presupuestaria",
                partida.getId(), "{\"porcentaje\": " + partida.getPorcentajeEjecucion() + "}");
    }
}
