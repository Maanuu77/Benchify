package com.benchify.banco.domain;

/** Dirección orientativa del banco; todos los campos son opcionales salvo el país. */
public record DireccionPostal(String direccion, String ciudad, String provincia, String pais) {

    public static final String PAIS_POR_DEFECTO = "España";

    public DireccionPostal {
        direccion = limpiar(direccion);
        ciudad = limpiar(ciudad);
        provincia = limpiar(provincia);
        pais = limpiar(pais) == null ? PAIS_POR_DEFECTO : pais.strip();
    }

    private static String limpiar(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
