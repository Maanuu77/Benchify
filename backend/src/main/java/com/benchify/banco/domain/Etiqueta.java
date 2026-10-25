package com.benchify.banco.domain;

/** Etiqueta del catálogo (romántico, vistas, sombra…). El catálogo lo gestiona Flyway. */
public record Etiqueta(Long id, String nombre, String color, String icono) {
}
