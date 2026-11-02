package com.benchify.banco.domain;

import com.benchify.shared.domain.NotFoundException;

import java.util.UUID;

public class RevisionNotFoundException extends NotFoundException {

    public RevisionNotFoundException(UUID id) {
        super("REVISION_NOT_FOUND", "Edición no encontrada: " + id);
    }
}
