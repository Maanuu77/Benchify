package com.benchify.banco.domain;

/** Atributos que se pueden filtrar en el mapa. {@code material} es opcional. */
public record Caracteristicas(boolean conSombra, boolean accesible, boolean conRespaldo, MaterialBanco material) {
}
