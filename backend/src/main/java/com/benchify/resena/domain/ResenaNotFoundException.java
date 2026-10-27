package com.benchify.resena.domain;

import com.benchify.shared.domain.NotFoundException;

import java.util.UUID;

public class ResenaNotFoundException extends NotFoundException {

    public ResenaNotFoundException(UUID id) {
        super("RESENA_NOT_FOUND", "Reseña no encontrada: " + id);
    }
}
