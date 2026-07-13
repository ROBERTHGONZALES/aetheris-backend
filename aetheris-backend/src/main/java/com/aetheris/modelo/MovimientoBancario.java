package com.aetheris.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "movimiento_bancario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoBancario {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conciliacion_id", nullable = false)
    private ConciliacionBancaria conciliacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id")
    private TransaccionFinanciera transaccion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(length = 100)
    private String referencia;

    @Column(name = "descripcion_banco", columnDefinition = "TEXT")
    private String descripcionBanco;

    @Column(name = "estado_conciliacion", nullable = false)
    private Boolean estadoConciliacion = false;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
    }

    public boolean tieneConciliado() {
        return Boolean.TRUE.equals(estadoConciliacion);
    }
}
