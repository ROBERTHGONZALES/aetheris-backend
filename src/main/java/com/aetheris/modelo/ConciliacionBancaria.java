package com.aetheris.modelo;

import com.aetheris.modelo.enums.EstadoConciliacion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conciliacion_bancaria")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConciliacionBancaria {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_bancaria_id", nullable = false)
    private CuentaBancaria cuentaBancaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String periodo;

    @Column(name = "fecha_ejecucion", nullable = false)
    private LocalDateTime fechaEjecucion;

    @Column(name = "total_conciliados", nullable = false)
    private Integer totalConciliados = 0;

    @Column(name = "total_discrepancias", nullable = false)
    private Integer totalDiscrepancias = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoConciliacion estado = EstadoConciliacion.EN_PROCESO;

    @OneToMany(mappedBy = "conciliacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MovimientoBancario> movimientos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (fechaEjecucion == null) fechaEjecucion = LocalDateTime.now();
    }

    public void agregarMovimiento(MovimientoBancario m) {
        movimientos.add(m);
        m.setConciliacion(this);
    }

    public void incrementarConciliados() { totalConciliados++; }
    public void incrementarDiscrepancias() { totalDiscrepancias++; }
}
