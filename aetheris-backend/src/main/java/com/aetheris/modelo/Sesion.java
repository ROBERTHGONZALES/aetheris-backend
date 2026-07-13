package com.aetheris.modelo;

import com.aetheris.modelo.enums.EstadoSesion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sesion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sesion {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_sesion", nullable = false, length = 10)
    private EstadoSesion estadoSesion = EstadoSesion.ACTIVA;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
    }

    public boolean isActiva() {
        return EstadoSesion.ACTIVA.equals(estadoSesion);
    }
}
