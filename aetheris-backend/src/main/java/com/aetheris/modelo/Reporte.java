package com.aetheris.modelo;

import com.aetheris.modelo.enums.FormatoReporte;
import com.aetheris.modelo.enums.TipoReporte;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reporte")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reporte {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoReporte tipo;

    @Column(length = 20)
    private String periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormatoReporte formato;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "url_archivo", nullable = false, length = 500)
    private String urlArchivo;

    @Column(columnDefinition = "JSON")
    private String parametros;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (fechaGeneracion == null) fechaGeneracion = LocalDateTime.now();
    }
}
