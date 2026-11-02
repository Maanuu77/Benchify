package com.benchify.lugar.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Ubicacion;

import java.util.Locale;

/**
 * Lo que se pide al geocodificador, ya limpio: el texto sin espacios repetidos, el idioma de los
 * resultados y, si se conoce, un punto cerca del que priorizar (el centro del mapa).
 */
public record Busqueda(String texto, String idioma, Ubicacion cerca) {

    public static final int MIN_TEXTO = 3;
    public static final int MAX_TEXTO = 120;

    public static Busqueda de(String texto, String idioma, Ubicacion cerca) {
        String limpio = texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
        if (limpio.length() < MIN_TEXTO || limpio.length() > MAX_TEXTO) {
            throw new BusinessRuleException("BUSQUEDA_INVALIDA",
                    "Escribe entre " + MIN_TEXTO + " y " + MAX_TEXTO + " caracteres");
        }
        String lengua = idioma != null && idioma.toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "es";
        return new Busqueda(limpio, lengua, cerca);
    }

    /** Dos búsquedas con la misma clave dan los mismos resultados: el punto se redondea a ~10 km. */
    public String clave() {
        String zona = cerca == null ? "-" : Math.round(cerca.lat() * 10) + "," + Math.round(cerca.lng() * 10);
        return idioma + "|" + zona + "|" + texto.toLowerCase(Locale.ROOT);
    }
}
