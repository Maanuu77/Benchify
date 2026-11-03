package com.benchify.resena.domain;

import com.benchify.shared.domain.NotFoundException;

import java.util.UUID;

public class ResenaNotFoundException extends NotFoundException {

    public ResenaNotFoundException(UUID id) {
        super("RESENA_NOT_FOUND", "Reseña no encontrada: " + id);
    }

    private ResenaNotFoundException(String code, String message) {
        super(code, message);
    }

    /** El usuario todavía no ha reseñado el banco. */
    public static ResenaNotFoundException sinResenaPropia(UUID bancoId) {
        return new ResenaNotFoundException("RESENA_NO_ENCONTRADA", "Todavía no has reseñado el banco " + bancoId);
    }
}
