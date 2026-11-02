package com.benchify.lugar.domain;

import com.benchify.shared.domain.Ubicacion;

/**
 * Resultado de buscar una dirección o un sitio por su nombre.
 *
 * @param nombre    nombre corto ("Plaza de España")
 * @param direccion descripción completa, con ciudad y provincia
 * @param encuadre  zona que ocupa; null si solo se conoce el punto
 */
public record Lugar(String nombre, String direccion, Ubicacion ubicacion, Encuadre encuadre) {
}
