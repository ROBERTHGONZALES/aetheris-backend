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
import java.util.List;

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
}
