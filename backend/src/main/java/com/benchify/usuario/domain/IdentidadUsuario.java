package com.benchify.usuario.domain;

import com.benchify.shared.domain.Rol;

import java.util.Objects;

/** Datos de identidad que aporta el proveedor (Firebase) en cada inicio de sesión. */
public record IdentidadUsuario(String id, String email, String nombre, String avatarUrl, Rol rol) {

    public IdentidadUsuario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rol, "rol");
    }
}
