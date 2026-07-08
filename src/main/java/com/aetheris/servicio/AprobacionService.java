package com.aetheris.servicio;

import com.aetheris.dao.FlujodeAprobacionDAO;
import com.aetheris.dao.TransaccionFinancieraDAO;
import com.aetheris.modelo.FlujodeAprobacion;
import com.aetheris.modelo.TransaccionFinanciera;
import com.aetheris.modelo.Usuario;
import com.aetheris.modelo.enums.EstadoAprobacion;
import com.aetheris.modelo.enums.EstadoFlujo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AprobacionService {

    private final FlujodeAprobacionDAO flujoDAO;
    private final TransaccionFinancieraDAO transaccionDAO;
    private final AuditoriaService auditoriaService;

    @Transactional
    public FlujodeAprobacion generarFlujo(TransaccionFinanciera tx) {
        FlujodeAprobacion flujo = FlujodeAprobacion.builder()
                .transaccion(tx)
                .montoLimite(tx.getSede().getMontoLimiteAprobacion())
                .estado(EstadoFlujo.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();
        FlujodeAprobacion saved = flujoDAO.save(flujo);
        auditoriaService.registrar(tx.getUsuarioRegistro(), "GENERAR_FLUJO_APROBACION",
                "APROBACION", "FLUJO_APROBACION", null,
                "flujo_aprobacion", saved.getId(), null);
        return saved;
    }

    @Transactional
    public void aprobarTransaccion(String idFlujo, Usuario cfo, String observacion) {
        validarSegregacion(idFlujo, cfo);
        FlujodeAprobacion flujo = obtenerFlujo(idFlujo);
        flujo.setEstado(EstadoFlujo.APROBADO);
        flujo.setUsuarioAprobador(cfo);
        flujo.setFechaResolucion(LocalDateTime.now());
        flujo.setObservacion(observacion);
        flujoDAO.save(flujo);

        TransaccionFinanciera tx = flujo.getTransaccion();
        tx.setEstadoAprobacion(EstadoAprobacion.APROBADA);
        tx.setUsuarioAprobador(cfo);
        transaccionDAO.save(tx);

        auditoriaService.registrar(cfo, "APROBAR_TRANSACCION", "APROBACION",
                "TRANSACCION", null, "transaccion_financiera", tx.getId(), null);
    }

    @Transactional
    public void rechazarTransaccion(String idFlujo, Usuario cfo, String observacion) {
        validarSegregacion(idFlujo, cfo);
        FlujodeAprobacion flujo = obtenerFlujo(idFlujo);
        flujo.setEstado(EstadoFlujo.RECHAZADO);
        flujo.setUsuarioAprobador(cfo);
        flujo.setFechaResolucion(LocalDateTime.now());
        flujo.setObservacion(observacion);
        flujoDAO.save(flujo);

        TransaccionFinanciera tx = flujo.getTransaccion();
        tx.setEstadoAprobacion(EstadoAprobacion.RECHAZADA);
        transaccionDAO.save(tx);

        auditoriaService.registrar(cfo, "RECHAZAR_TRANSACCION", "APROBACION",
                "TRANSACCION", null, "transaccion_financiera", tx.getId(), null);
    }

    @Transactional(readOnly = true)
    public List<FlujodeAprobacion> listarFlujosPendientes() {
        return flujoDAO.findByEstadoOrderByFechaSolicitudAsc(EstadoFlujo.PENDIENTE);
    }

    private FlujodeAprobacion obtenerFlujo(String idFlujo) {
        return flujoDAO.findById(idFlujo)
                .orElseThrow(() -> new RuntimeException("Flujo no encontrado: " + idFlujo));
    }

    private void validarSegregacion(String idFlujo, Usuario aprobador) {
        FlujodeAprobacion flujo = obtenerFlujo(idFlujo);
        String registradorId = flujo.getTransaccion().getUsuarioRegistro().getId();
        if (registradorId.equals(aprobador.getId())) {
            throw new RuntimeException(
                    "Segregación de funciones: el registrador no puede aprobar su propia transacción");
        }
    }
}
