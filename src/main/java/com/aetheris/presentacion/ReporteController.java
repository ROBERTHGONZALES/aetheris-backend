package com.aetheris.presentacion;

import com.aetheris.modelo.Reporte;
import com.aetheris.modelo.Usuario;
import com.aetheris.modelo.enums.FormatoReporte;
import com.aetheris.servicio.AutenticacionService;
import com.aetheris.servicio.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Generación de reportes: ADMIN, CONTADOR y AUDITOR (APROBADOR no genera reportes). */
@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','CONTADOR','AUDITOR')")
public class ReporteController {

    private final ReporteService reporteService;
    private final AutenticacionService autenticacionService;

    /** POST /api/reportes/ingresos-egresos */
    @PostMapping("/ingresos-egresos")
    public ResponseEntity<Reporte> generarIngresosEgresos(
            @RequestParam String periodo,
            @RequestParam String sedeId,
            @RequestParam(defaultValue = "PDF") FormatoReporte formato,
            @RequestHeader("Authorization") String auth) {
        Usuario u = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.ok(reporteService.generarReporteIngresosEgresos(periodo, sedeId, formato, u));
    }

    /** POST /api/reportes/presupuestal */
    @PostMapping("/presupuestal")
    public ResponseEntity<Reporte> generarPresupuestal(
            @RequestParam String periodo,
            @RequestParam String sedeId,
            @RequestParam(defaultValue = "PDF") FormatoReporte formato,
            @RequestHeader("Authorization") String auth) {
        Usuario u = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.ok(reporteService.generarReportePresupuestal(periodo, sedeId, formato, u));
    }

    /** POST /api/reportes/conciliacion */
    @PostMapping("/conciliacion")
    public ResponseEntity<Reporte> generarConciliacion(
            @RequestParam String idConciliacion,
            @RequestParam(defaultValue = "PDF") FormatoReporte formato,
            @RequestHeader("Authorization") String auth) {
        Usuario u = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.ok(reporteService.generarReporteConciliacion(idConciliacion, formato, u));
    }

    /** POST /api/reportes/auditoria */
    @PostMapping("/auditoria")
    public ResponseEntity<Reporte> generarAuditoria(
            @RequestParam LocalDateTime inicio,
            @RequestParam LocalDateTime fin,
            @RequestParam(defaultValue = "PDF") FormatoReporte formato,
            @RequestHeader("Authorization") String auth) {
        Usuario u = autenticacionService.obtenerUsuarioPorToken(extraerToken(auth));
        return ResponseEntity.ok(reporteService.generarReporteAuditoria(inicio, fin, formato, u));
    }

    /** GET /api/reportes?usuarioId=xxx */
    @GetMapping
    public ResponseEntity<List<Reporte>> listarReportes(
            @RequestParam String usuarioId) {
        return ResponseEntity.ok(reporteService.listarReportesPorUsuario(usuarioId));
    }

    private String extraerToken(String header) {
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        throw new RuntimeException("Token no proporcionado");
    }
}
