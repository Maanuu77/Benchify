package com.benchify.resena.domain;

/** Agregado de las reseñas de un banco, para desnormalizarlo en la ficha. */
public record ResumenPuntuacion(double media, int total) {

    public static final ResumenPuntuacion VACIO = new ResumenPuntuacion(0, 0);
}
