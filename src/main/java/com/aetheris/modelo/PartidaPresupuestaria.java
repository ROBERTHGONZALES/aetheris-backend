package com.aetheris.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partida_presupuestaria",
    uniqueConstraints = @UniqueConstraint(columnNames = {"sede_id","categoria_id","periodo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartidaPresupuestaria {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaContable categoria;

    @Column(nullable = false, length = 20)
    private String periodo;

    @Column(name = "monto_presupuestado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoPresupuestado;

    @Column(name = "monto_ejecutado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoEjecutado = BigDecimal.ZERO;

    // Columna generada en BD — solo lectura desde Java
    @Column(name = "porcentaje_ejecucion", insertable = false, updatable = false, precision = 6, scale = 2)
    private BigDecimal porcentajeEjecucion;

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

    public void actualizarEjecucion(BigDecimal monto) {
        this.montoEjecutado = this.montoEjecutado.add(monto);
    }

    public boolean superaUmbralAlerta() {
        if (montoPresupuestado.compareTo(BigDecimal.ZERO) == 0) return false;
        BigDecimal porcentaje = montoEjecutado
            .divide(montoPresupuestado, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        return porcentaje.compareTo(BigDecimal.valueOf(90)) >= 0;
    }

    public BigDecimal calcularSaldoDisponible() {
        return montoPresupuestado.subtract(montoEjecutado);
    }
}
