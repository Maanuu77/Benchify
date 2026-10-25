package com.benchify.shared.infrastructure.security;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Rol;

/** Datos del usuario extraídos de un ID token válido. */
public record AuthenticatedUser(
        String uid,
        String email,
        String name,
        String picture,
        boolean emailVerified,
        Rol rol) {

    public Actor toActor() {
        return new Actor(uid, rol);
    }
}
