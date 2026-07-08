package com.aetheris.servicio;

import com.aetheris.dao.*;
import com.aetheris.modelo.*;
import com.aetheris.modelo.enums.EstadoDiscrepancia;
import com.aetheris.modelo.enums.TipoResolucionDisc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConciliacionService {

    private final ConciliacionBancariaDAO conciliacionDAO;
    private final MovimientoBancarioDAO movimientoDAO;
    private final DiscrepanciaDAO discrepanciaDAO;
    private final TransaccionFinancieraDAO transaccionDAO;
    private final CuentaBancariaDAO cuentaDAO;
    private final AuditoriaService auditoriaService;

    @Transactional
    public ConciliacionBancaria iniciarConciliacion(String cuentaId,
                                                     String periodo, Usuario usuario) {
        CuentaBancaria cuenta = cuentaDAO.findById(cuentaId)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + cuentaId));
        ConciliacionBancaria c = ConciliacionBancaria.builder()
                .cuentaBancaria(cuenta)
                .usuario(usuario)
                .periodo(periodo)
                .fechaEjecucion(LocalDateTime.now())
                .build();
        ConciliacionBancaria saved = conciliacionDAO.save(c);
        auditoriaService.registrar(usuario, "INICIAR_CONCILIACION", "CONCILIACION",
                "CONCILIACION_BANCARIA", null, "conciliacion_bancaria", saved.getId(), null);
        return saved;
    }

    @Transactional
    public List<MovimientoBancario> importarMovimientos(String idConc,
                                                        List<MovimientoBancario> movimientos) {
        ConciliacionBancaria conc = conciliacionDAO.findById(idConc)
                .orElseThrow(() -> new RuntimeException("Conciliación no encontrada: " + idConc));
        movimientos.forEach(m -> {
            m.setConciliacion(conc);
            movimientoDAO.save(m);
        });
        return movimientos;
    }

    @Transactional
    public void ejecutarCruce(String idConc) {
        ConciliacionBancaria conc = conciliacionDAO.findById(idConc)
                .orElseThrow(() -> new RuntimeException("Conciliación no encontrada: " + idConc));
        List<MovimientoBancario> movimientos = movimientoDAO.findByConciliacionId(idConc);

        movimientos.forEach(mov -> {
            // Buscar transacción interna que coincida por monto y fecha
            boolean coincide = transaccionDAO
                    .findByFechaBetweenOrderByFechaDesc(mov.getFecha(), mov.getFecha())
                    .stream()
                    .anyMatch(tx -> tx.getMonto().compareTo(mov.getMonto()) == 0);

            if (coincide) {
                mov.setEstadoConciliacion(true);
                conc.incrementarConciliados();
            } else {
                mov.setEstadoConciliacion(false);
                conc.incrementarDiscrepancias();
                Discrepancia disc = Discrepancia.builder()
                        .movimientoBancario(mov)
                        .estado(EstadoDiscrepancia.ABIERTA)
                        .build();
                discrepanciaDAO.save(disc);
            }
            movimientoDAO.save(mov);
        });
        conciliacionDAO.save(conc);
    }

    @Transactional(readOnly = true)
    public List<Discrepancia> listarDiscrepancias(String idConc) {
        List<MovimientoBancario> movs = movimientoDAO.findByConciliacionId(idConc);
        return movs.stream()
                .flatMap(m -> discrepanciaDAO.findByMovimientoBancarioId(m.getId()).stream())
                .toList();
    }

    @Transactional
    public void resolverDiscrepancia(String idDisc, TipoResolucionDisc tipo,
                                     String justificacion, Usuario usuario) {
        Discrepancia disc = discrepanciaDAO.findById(idDisc)
                .orElseThrow(() -> new RuntimeException("Discrepancia no encontrada: " + idDisc));
        disc.setTipoResolucion(tipo);
        disc.setJustificacion(justificacion);
        disc.setEstado(EstadoDiscrepancia.RESUELTA);
        disc.setFechaResolucion(LocalDateTime.now());
        disc.setUsuarioResolucion(usuario);
        discrepanciaDAO.save(disc);
        auditoriaService.registrar(usuario, "RESOLVER_DISCREPANCIA", "CONCILIACION",
                "DISCREPANCIA", null, "discrepancia", idDisc, null);
    }
}
