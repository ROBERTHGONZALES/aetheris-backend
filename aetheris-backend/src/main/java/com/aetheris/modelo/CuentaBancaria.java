package com.aetheris.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cuenta_bancaria",
    uniqueConstraints = @UniqueConstraint(columnNames = {"entidad_bancaria","numero_cuenta"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CuentaBancaria {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(name = "entidad_bancaria", nullable = false, length = 100)
    private String entidadBancaria;

    @Column(name = "numero_cuenta", nullable = false, length = 50)
    private String numeroCuenta;

    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(name = "saldo_actual", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @Column(name = "saldo_contable", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoContable = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean estado = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
