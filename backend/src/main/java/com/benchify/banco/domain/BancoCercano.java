package com.benchify.banco.domain;

/** Resultado de la búsqueda por cercanía. */
public record BancoCercano(Banco banco, double distanciaMetros) {
}
