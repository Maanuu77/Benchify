package com.benchify.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * DTO ligero para los marcadores del mapa.
 * Minimiza el payload JSON en el endpoint de bounding box.
 */
@Getter
@Builder
public class BancoMapaResponse {
    private UUID id;
    private String titulo;
    private double lat;
    private double lng;
    private double puntuacionMedia;
    private int numResenas;
    private boolean accesible;
    private boolean conSombra;
    private List<String> etiquetas;
    private String primeraImagenUrl;
}
