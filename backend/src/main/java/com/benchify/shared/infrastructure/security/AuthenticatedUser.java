package com.benchify.shared.infrastructure.security;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Rol;

/**
 * Datos del usuario extraídos de un ID token válido.
 *
 * @param rol rol con el que se autoriza. El verificador lo rellena con el custom claim del token y
 *            {@link FirebaseAuthenticationFilter} lo sustituye por el de la base de datos
 *            ({@link RoleResolver}) antes de autorizar la petición
 */
public record AuthenticatedUser(
        String uid,
        String email,
        String name,
        String picture,
        boolean emailVerified,
        Rol rol) {

    public AuthenticatedUser withRol(Rol nuevoRol) {
        return new AuthenticatedUser(uid, email, name, picture, emailVerified, nuevoRol);
    }

    public Actor toActor() {
        return new Actor(uid, rol);
    }
}
