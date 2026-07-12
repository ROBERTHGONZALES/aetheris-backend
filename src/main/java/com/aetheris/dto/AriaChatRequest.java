package com.aetheris.dto;

import lombok.Data;

import java.util.List;

@Data
public class AriaChatRequest {
    private String message;
    private List<AriaHistoryMessage> history;
}
