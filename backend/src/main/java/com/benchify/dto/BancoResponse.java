package com.benchify.dto;

import com.benchify.domain.enums.EstadoBanco;
import com.benchify.domain.enums.MaterialBanco;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class BancoResponse {
    private UUID id;
    private String titulo;
    private String descripcion;
    private double lat;
    private double lng;
    private String direccion;
    private String ciudad;
    private String provincia;
    private String pais;
    private EstadoBanco estado;
    private boolean conSombra;
    private boolean accesible;
    private boolean conRespaldo;
    private MaterialBanco material;
    private double puntuacionMedia;
    private int numResenas;
    private LocalDateTime fechaSubida;
    private String creadoPorNombre;
    private List<String> etiquetas;
    private List<String> imagenesUrls;
}
