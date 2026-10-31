package com.benchify.shared.domain;

/**
 * La petición partía de una versión del recurso que ya no es la actual (p. ej. una edición
 * con {@code If-Match} sobre una copia antigua).
 */
public class PreconditionFailedException extends DomainException {

    public PreconditionFailedException(String code, String message) {
        super(code, message);
    }
}
