package com.benchify.shared.domain;

/** Petición bien formada que incumple una regla de negocio (p. ej. reseñar un banco no publicado). */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
