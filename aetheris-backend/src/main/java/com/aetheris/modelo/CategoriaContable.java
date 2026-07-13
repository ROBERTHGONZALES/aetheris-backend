package com.aetheris.modelo;

import com.aetheris.modelo.enums.TipoTransaccion;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "categoria_contable")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoriaContable {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransaccion tipo;

    @Column(nullable = false)
    private Boolean estado = true;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
