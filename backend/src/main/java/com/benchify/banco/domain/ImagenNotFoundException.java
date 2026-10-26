package com.benchify.banco.domain;

import com.benchify.shared.domain.NotFoundException;

import java.util.UUID;

public class ImagenNotFoundException extends NotFoundException {

    public ImagenNotFoundException(UUID id) {
        super("IMAGEN_NOT_FOUND", "Imagen no encontrada: " + id);
    }
}
