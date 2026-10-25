package com.benchify.shared.domain;

/**
 * Error de negocio. Cada subtipo se traduce a un código HTTP en el adaptador web;
 * {@link #code()} es un identificador estable que el cliente puede usar para mostrar
 * mensajes propios.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
