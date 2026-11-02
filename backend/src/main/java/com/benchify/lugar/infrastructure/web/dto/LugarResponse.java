package com.benchify.lugar.infrastructure.web.dto;

/** Un resultado de la búsqueda; los límites del encuadre son null si solo se conoce el punto. */
public record LugarResponse(String nombre, String direccion, double lat, double lng,
        Double sur, Double oeste, Double norte, Double este) {
}
