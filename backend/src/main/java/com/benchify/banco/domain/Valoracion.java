package com.benchify.banco.domain;

/** Media de puntuación y número de reseñas, desnormalizados para pintar el mapa sin agregaciones. */
public record Valoracion(double media, int numResenas) {

    public static final Valoracion SIN_RESENAS = new Valoracion(0, 0);

    public Valoracion {
        if (numResenas < 0 || media < 0 || media > 5) {
            throw new IllegalArgumentException("Invalid rating: " + media + " / " + numResenas);
        }
        // Una decimal es suficiente para mostrar y ordenar
        media = Math.round(media * 10.0) / 10.0;
    }
}
