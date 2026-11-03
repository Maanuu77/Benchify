package com.benchify.shared.domain;

/** Un servicio externo del que depende la operación no responde o no admite más peticiones ahora. */
public class ServiceUnavailableException extends DomainException {

    public ServiceUnavailableException(String code, String message) {
        super(code, message);
    }
}
