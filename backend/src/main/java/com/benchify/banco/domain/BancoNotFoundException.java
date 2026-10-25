package com.benchify.banco.domain;

import com.benchify.shared.domain.NotFoundException;

import java.util.UUID;

public class BancoNotFoundException extends NotFoundException {

    public BancoNotFoundException(UUID id) {
        super("BANCO_NOT_FOUND", "Banco no encontrado: " + id);
    }
}
