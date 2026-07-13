package com.aetheris.servicio;

import com.aetheris.dao.ReporteDAO;
import com.aetheris.dao.TransaccionFinancieraDAO;
import com.aetheris.modelo.Reporte;
import com.aetheris.modelo.Usuario;
import com.aetheris.modelo.enums.FormatoReporte;
import com.aetheris.modelo.enums.TipoReporte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteDAO reporteDAO;
    private final TransaccionFinancieraDAO transaccionDAO;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Reporte generarReporteIngresosEgresos(String periodo, String idSede,
                                                  FormatoReporte formato, Usuario usuario) {
        return guardarReporte(TipoReporte.INGRESOS_EGRESOS, periodo, formato, usuario,
                "{\"sede\": \"" + idSede + "\", \"periodo\": \"" + periodo + "\"}");
    }

    @Transactional
    public Reporte generarReportePresupuestal(String periodo, String idSede,
                                               FormatoReporte formato, Usuario usuario) {
        return guardarReporte(TipoReporte.PRESUPUESTAL, periodo, formato, usuario,
                "{\"sede\": \"" + idSede + "\", \"periodo\": \"" + periodo + "\"}");
    }

    @Transactional
    public Reporte generarReporteConciliacion(String idConc, FormatoReporte formato,
                                               Usuario usuario) {
        return guardarReporte(TipoReporte.CONCILIACION, null, formato, usuario,
                "{\"conciliacion_id\": \"" + idConc + "\"}");
    }

    @Transactional
    public Reporte generarReporteAuditoria(LocalDateTime inicio, LocalDateTime fin,
                                            FormatoReporte formato, Usuario usuario) {
        return guardarReporte(TipoReporte.AUDITORIA, null, formato, usuario,
                "{\"inicio\": \"" + inicio + "\", \"fin\": \"" + fin + "\"}");
    }

    @Transactional(readOnly = true)
    public Reporte obtenerReporte(String id) {
        return reporteDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reporte> listarReportesPorUsuario(String idUsuario) {
        return reporteDAO.findByUsuarioIdOrderByFechaGeneracionDesc(idUsuario);
    }

    private Reporte guardarReporte(TipoReporte tipo, String periodo,
                                    FormatoReporte formato, Usuario usuario,
                                    String parametros) {
        // URL simulada — en producción apuntará al archivo generado real
        String url = "/reportes/" + UUID.randomUUID() + "."
                + formato.name().toLowerCase();
        Reporte reporte = Reporte.builder()
                .usuario(usuario)
                .tipo(tipo)
                .periodo(periodo)
                .formato(formato)
                .urlArchivo(url)
                .parametros(parametros)
                .fechaGeneracion(LocalDateTime.now())
                .build();
        Reporte saved = reporteDAO.save(reporte);
        auditoriaService.registrar(usuario, "GENERAR_REPORTE", "REPORTES",
                "REPORTE", null, "reporte", saved.getId(),
                "{\"tipo\": \"" + tipo + "\"}");
        return saved;
    }
}
