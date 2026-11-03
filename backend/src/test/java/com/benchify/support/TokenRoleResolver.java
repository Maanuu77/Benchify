package com.benchify.support;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.shared.infrastructure.security.RoleResolver;

/**
 * En los tests de slice no hay base de datos de usuarios: se autoriza con el rol del token, que
 * los tests eligen con {@link StubIdTokenVerifier#bearerAdmin}.
 */
public class TokenRoleResolver implements RoleResolver {

    @Override
    public Rol resolve(AuthenticatedUser user) {
        return user.rol();
    }
}
