package com.benchify.shared.domain;

/** El recurso pedido no existe o no es visible para quien lo pide. */
public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
