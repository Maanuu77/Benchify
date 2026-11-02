package com.benchify.lugar.domain;

/** Rectángulo que abarca un lugar (una calle, un barrio, una ciudad) para encuadrarlo en el mapa. */
public record Encuadre(double sur, double oeste, double norte, double este) {
}
