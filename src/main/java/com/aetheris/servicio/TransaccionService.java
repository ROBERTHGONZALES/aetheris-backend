package com.aetheris.servicio;

import com.aetheris.dao.TransaccionFinancieraDAO;
import com.aetheris.modelo.TransaccionFinanciera;
import com.aetheris.modelo.Usuario;
import com.aetheris.modelo.enums.EstadoAprobacion;
import com.aetheris.modelo.enums.TipoTransaccion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransaccionService {

    private final TransaccionFinancieraDAO transaccionDAO;
    private final PresupuestoService presupuestoService;
    private final AprobacionService aprobacionService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public TransaccionFinanciera registrarTransaccion(TransaccionFinanciera tx, Usuario usuario) {
        tx.setUsuarioRegistro(usuario);
        TransaccionFinanciera saved = transaccionDAO.save(tx);

        // Si supera el límite de aprobación, generar flujo automáticamente
        if (saved.requiereAprobacion(saved.getSede().getMontoLimiteAprobacion())) {
            aprobacionService.generarFlujo(saved);
        } else {
            // Aprobación automática para montos dentro del límite
            saved.setEstadoAprobacion(EstadoAprobacion.APROBADA);
            transaccionDAO.save(saved);

            // Si es egreso aprobado, actualizar partida presupuestaria
            if (TipoTransaccion.EGRESO.equals(saved.getTipo())) {
                try {
                    String periodo = saved.getFecha().getYear() + "-"
                            + String.format("%02d", saved.getFecha().getMonthValue());
                    presupuestoService.actualizarEjecucion(
                            saved.getCategoria().getId(), saved.getMonto());
                } catch (Exception ignored) {
                    // Partida puede no existir para ese período/categoría
                }
            }
        }

        auditoriaService.registrar(usuario, "REGISTRAR_TRANSACCION", "TRANSACCIONES",
                "TRANSACCION", null, "transaccion_financiera", saved.getId(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public TransaccionFinanciera obtenerTransaccion(String id) {
        return transaccionDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<TransaccionFinanciera> listarTransaccionesPorSede(String idSede) {
        if (idSede == null || idSede.isBlank()) {
            return transaccionDAO.findAllByOrderByFechaDesc();
        }
        return transaccionDAO.findBySedeId(idSede);
    }

    @Transactional(readOnly = true)
    public List<TransaccionFinanciera> listarTransaccionesPendientes() {
        return transaccionDAO.findByEstadoAprobacion(EstadoAprobacion.PENDIENTE);
    }

    @Transactional(readOnly = true)
    public List<TransaccionFinanciera> listarTransaccionesPorPeriodo(
            LocalDate inicio, LocalDate fin) {
        return transaccionDAO.findByFechaBetweenOrderByFechaDesc(inicio, fin);
    }

    // ── Métodos analíticos ─────────────────────────────────────────────────

    /**
     * Resumen de ingresos y egresos agrupado por mes y año de todo el historial.
     * Ideal para preguntas como "¿qué mes tuvo más ingresos?" o
     * "¿cómo ha evolucionado el flujo financiero?"
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resumenMensual() {
        List<Object[]> rows = transaccionDAO.resumenMensual();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("anio",     row[0]);
            entry.put("mes",      row[1]);
            entry.put("tipo",     row[2] != null ? row[2].toString() : "");
            entry.put("total",    row[3]);
            entry.put("cantidad", row[4]);
            result.add(entry);
        }
        return result;
    }

    /**
     * Totales de ingresos y egresos agrupados por sede.
     * Ideal para comparar el desempeño financiero entre sedes.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resumenPorSede() {
        List<Object[]> rows = transaccionDAO.resumenPorSede();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("sede",     row[0]);
            entry.put("tipo",     row[1] != null ? row[1].toString() : "");
            entry.put("total",    row[2]);
            entry.put("cantidad", row[3]);
            result.add(entry);
        }
        return result;
    }

    /**
     * Totales de ingresos y egresos agrupados por categoría contable.
     * Ideal para analizar en qué categorías se concentra el gasto o ingreso.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resumenPorCategoria() {
        List<Object[]> rows = transaccionDAO.resumenPorCategoria();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("categoria", row[0]);
            entry.put("tipo",      row[1] != null ? row[1].toString() : "");
            entry.put("total",     row[2]);
            entry.put("cantidad",  row[3]);
            result.add(entry);
        }
        return result;
    }
}
