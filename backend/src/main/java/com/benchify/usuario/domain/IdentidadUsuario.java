package com.benchify.usuario.domain;

import com.benchify.shared.domain.Rol;

import java.util.Objects;

/**
 * Datos de identidad que aporta el proveedor (Firebase) en cada inicio de sesión.
 *
 * @param rol el del custom claim del token. Solo se usa al dar de alta al usuario (o al primer
 *            administrador); después manda el guardado en la base de datos
 */
public record IdentidadUsuario(String id, String email, String nombre, String avatarUrl, Rol rol) {

    public IdentidadUsuario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rol, "rol");
    }
}
