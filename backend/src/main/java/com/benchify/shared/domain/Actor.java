package com.benchify.shared.domain;

import java.util.Objects;

/**
 * Quién ejecuta un caso de uso. Los servicios de aplicación reciben un {@code Actor}
 * en lugar de depender de Spring Security.
 *
 * @param userId UID de Firebase del usuario autenticado
 */
public record Actor(String userId, Rol rol) {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(rol, "rol");
    }

    public static Actor user(String userId) {
        return new Actor(userId, Rol.USER);
    }

    public static Actor admin(String userId) {
        return new Actor(userId, Rol.ADMIN);
    }

    public boolean isAdmin() {
        return rol == Rol.ADMIN;
    }

    /** {@code true} si el actor es el propietario del recurso o un administrador. */
    public boolean canManage(String ownerId) {
        return isAdmin() || userId.equals(ownerId);
    }
}
