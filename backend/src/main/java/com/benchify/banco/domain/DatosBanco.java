package com.benchify.banco.domain;

import com.benchify.shared.domain.BusinessRuleException;

import java.util.Objects;
import java.util.Set;

/** Contenido editable de un banco. Valida sus invariantes al construirse. */
public record DatosBanco(
        String titulo,
        String descripcion,
        Ubicacion ubicacion,
        DireccionPostal direccion,
        Caracteristicas caracteristicas,
        Set<Etiqueta> etiquetas) {

    public static final int MAX_TITULO = 150;
    public static final int MAX_DESCRIPCION = 500;
    public static final int MAX_ETIQUETAS = 5;

    public DatosBanco {
        titulo = titulo == null ? "" : titulo.strip();
        if (titulo.isEmpty() || titulo.length() > MAX_TITULO) {
            throw new BusinessRuleException("BANCO_TITULO_INVALIDO",
                    "El título es obligatorio y no puede superar " + MAX_TITULO + " caracteres");
        }
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
        if (descripcion != null && descripcion.length() > MAX_DESCRIPCION) {
            throw new BusinessRuleException("BANCO_DESCRIPCION_INVALIDA",
                    "La descripción no puede superar " + MAX_DESCRIPCION + " caracteres");
        }
        Objects.requireNonNull(ubicacion, "ubicacion");
        direccion = direccion == null ? new DireccionPostal(null, null, null, null) : direccion;
        caracteristicas = caracteristicas == null ? new Caracteristicas(false, false, false, null) : caracteristicas;
        etiquetas = etiquetas == null ? Set.of() : Set.copyOf(etiquetas);
        if (etiquetas.size() > MAX_ETIQUETAS) {
            throw new BusinessRuleException("BANCO_DEMASIADAS_ETIQUETAS",
                    "Un banco admite como máximo " + MAX_ETIQUETAS + " etiquetas");
        }
    }
}
