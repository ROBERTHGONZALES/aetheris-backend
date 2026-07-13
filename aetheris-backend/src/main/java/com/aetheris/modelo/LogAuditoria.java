package com.aetheris.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "log_auditoria")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class LogAuditoria {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(length = 100)
    private String categoria;

    @Column(length = 100)
    private String modulo;

    @Column(name = "entidad_afectada", length = 100)
    private String entidadAfectada;

    @Column(name = "entidad_id", columnDefinition = "CHAR(36)")
    private String entidadId;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(columnDefinition = "JSON")
    private String detalle;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }

    // Inmutable: no hay setters (Lombok @Getter sin @Setter)
}
