package com.benchify.shared.domain;

/** El actor está autenticado pero no puede realizar la operación sobre este recurso. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String code, String message) {
        super(code, message);
    }
}
