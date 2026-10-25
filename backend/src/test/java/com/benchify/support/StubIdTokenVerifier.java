package com.benchify.support;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.shared.infrastructure.security.IdTokenVerifier;
import com.benchify.shared.infrastructure.security.InvalidIdTokenException;

/**
 * Verificador para tests. Acepta tokens con formato {@code test:<uid>[:<ROL>]}, p. ej.
 * {@code Authorization: Bearer test:ana:ADMIN}; cualquier otro token es inválido.
 */
public class StubIdTokenVerifier implements IdTokenVerifier {

    private static final String PREFIX = "test:";

    public static String bearer(String uid) {
        return "Bearer " + PREFIX + uid;
    }

    public static String bearerAdmin(String uid) {
        return "Bearer " + PREFIX + uid + ":ADMIN";
    }

    @Override
    public AuthenticatedUser verify(String idToken) {
        if (!idToken.startsWith(PREFIX)) {
            throw new InvalidIdTokenException("Not a test token");
        }
        String[] parts = idToken.substring(PREFIX.length()).split(":");
        String uid = parts[0];
        Rol rol = parts.length > 1 ? Rol.fromClaim(parts[1]) : Rol.USER;
        return new AuthenticatedUser(uid, uid + "@benchify.test", "Usuario " + uid, null, true, rol);
    }
}
