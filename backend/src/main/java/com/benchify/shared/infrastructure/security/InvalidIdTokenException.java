package com.benchify.shared.infrastructure.security;

public class InvalidIdTokenException extends RuntimeException {

    public InvalidIdTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidIdTokenException(String message) {
        super(message);
    }
}
