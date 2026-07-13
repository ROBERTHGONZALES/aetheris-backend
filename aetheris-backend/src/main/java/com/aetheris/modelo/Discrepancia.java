package com.aetheris.modelo;

import com.aetheris.modelo.enums.EstadoDiscrepancia;
import com.aetheris.modelo.enums.TipoResolucionDisc;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "discrepancia")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Discrepancia {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_bancario_id", nullable = false)
    private MovimientoBancario movimientoBancario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_resolucion_id")
    private Usuario usuarioResolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_resolucion", length = 20)
    private TipoResolucionDisc tipoResolucion;

    @Column(columnDefinition = "TEXT")
    private String justificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoDiscrepancia estado = EstadoDiscrepancia.ABIERTA;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    public boolean estaResuelta() {
        return EstadoDiscrepancia.RESUELTA.equals(estado);
    }
}
