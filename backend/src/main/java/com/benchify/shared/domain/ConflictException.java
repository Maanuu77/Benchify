package com.benchify.shared.domain;

/** La operación choca con el estado actual (duplicado, ya existe…). */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
