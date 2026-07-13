package com.aetheris.modelo;

import com.aetheris.modelo.enums.EstadoFlujo;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flujo_aprobacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlujodeAprobacion {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id", nullable = false, unique = true)
    private TransaccionFinanciera transaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_aprobador_id")
    private Usuario usuarioAprobador;

    @Column(name = "monto_limite", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoFlujo estado = EstadoFlujo.PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (fechaSolicitud == null) fechaSolicitud = LocalDateTime.now();
    }

    public boolean esPendiente() {
        return EstadoFlujo.PENDIENTE.equals(estado);
    }
}
