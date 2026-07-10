package com.aetheris.dto;

import com.aetheris.modelo.LogAuditoria;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO de solo lectura para serializar logs de auditoría sin exponer
 * la entidad JPA completa. Resuelve el problema de usuario=null causado
 * por el proxy Hibernate lazy no inicializado al momento de la serialización.
 */
@Getter
@Builder
public class LogAuditoriaDTO {

    private String id;
    private String usuario;        // nombreCompleto del Usuario, o null para acciones del sistema
    private String correoUsuario;  // correoElectronico, o null para acciones del sistema
    private LocalDateTime fechaHora;
    private String accion;
    private String categoria;
    private String modulo;
    private String entidadAfectada;
    private String entidadId;
    private String direccionIp;
    private String detalle;

    public static LogAuditoriaDTO from(LogAuditoria log) {
        return LogAuditoriaDTO.builder()
                .id(log.getId())
                .usuario(log.getUsuario() != null ? log.getUsuario().getNombreCompleto() : null)
                .correoUsuario(log.getUsuario() != null ? log.getUsuario().getCorreoElectronico() : null)
                .fechaHora(log.getFechaHora())
                .accion(log.getAccion())
                .categoria(log.getCategoria())
                .modulo(log.getModulo())
                .entidadAfectada(log.getEntidadAfectada())
                .entidadId(log.getEntidadId())
                .direccionIp(log.getDireccionIp())
                .detalle(log.getDetalle())
                .build();
    }
}
