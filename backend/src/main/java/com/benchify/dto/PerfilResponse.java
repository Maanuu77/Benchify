package com.benchify.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PerfilResponse {
    private Long id;
    private String nombre;
    private String avatarUrl;
    private String ciudad;
    private long numBancosSubidos;
    private long numResenas;
    private long numFavoritos;
    private long numCheckIns;
    private List<BancoResponse> bancosRecientes;
}
