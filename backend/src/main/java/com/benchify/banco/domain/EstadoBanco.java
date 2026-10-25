package com.benchify.banco.domain;

/**
 * Ciclo de vida: un banco nuevo queda {@code PENDIENTE} hasta que un administrador lo
 * aprueba ({@code PUBLICADO}) o lo rechaza ({@code OCULTO}). El borrado es lógico ({@code ELIMINADO}).
 */
public enum EstadoBanco {
    PENDIENTE, PUBLICADO, OCULTO, ELIMINADO
}
