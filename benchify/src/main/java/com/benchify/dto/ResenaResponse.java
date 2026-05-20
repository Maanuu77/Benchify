package com.benchify.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResenaResponse {
    private Long id;
    private Integer puntuacion;
    private String comentario;
    private LocalDateTime fechaResena;
    private String nombreUsuario;
    private String avatarUrl;
    private boolean reportada;
    private int numReportes;
}
