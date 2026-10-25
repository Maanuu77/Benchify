package com.benchify.usuario.domain;

import com.benchify.shared.domain.NotFoundException;

public class UsuarioNotFoundException extends NotFoundException {

    public UsuarioNotFoundException(String id) {
        super("USUARIO_NOT_FOUND", "Usuario no encontrado: " + id);
    }
}
