package com.benchify.banco.infrastructure.web.dto;

import com.benchify.banco.domain.MaterialBanco;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record BancoRequest(
        @Schema(example = "Banco con vistas al Guadalquivir")
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String titulo,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @Schema(example = "37.3826")
        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "Latitud mínima: -90")
        @DecimalMax(value = "90.0", message = "Latitud máxima: 90")
        Double lat,

        @Schema(example = "-6.0034")
        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "Longitud mínima: -180")
        @DecimalMax(value = "180.0", message = "Longitud máxima: 180")
        Double lng,

        @Size(max = 255) String direccion,
        @Size(max = 100) String ciudad,
        @Size(max = 100) String provincia,
        @Size(max = 100) String pais,

        boolean conSombra,
        boolean accesible,
        boolean conRespaldo,
        MaterialBanco material,

        @Schema(description = "IDs del catálogo de etiquetas (máximo 5)")
        @Size(max = 5, message = "Un banco admite como máximo 5 etiquetas")
        Set<Long> etiquetaIds) {
}
