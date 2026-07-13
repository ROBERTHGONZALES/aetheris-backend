package com.aetheris.dto;

import lombok.Data;

/** Un turno previo de la conversación con ARIA, enviado por el cliente. */
@Data
public class AriaHistoryMessage {
    /** "user" o "model". */
    private String role;
    private String text;
}
