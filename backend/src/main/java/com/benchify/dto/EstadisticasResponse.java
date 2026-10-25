package com.benchify.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EstadisticasResponse {
    private long totalBancos;
    private long bancosPendientes;
    private long bancosPublicados;
    private long bancosOcultos;
    private long totalUsuarios;
    private long totalResenas;
    private long resenasPendientesModeracion;
}
