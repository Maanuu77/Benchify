package com.benchify.shared.infrastructure.security;

import com.benchify.shared.domain.Rol;

/**
 * Rol con el que se autoriza a un usuario autenticado. La fuente de verdad es la base de datos,
 * no el custom claim del token, que puede ser de antes de un cambio de rol. Lo implementa la
 * feature de usuarios, que además da de alta al usuario en su primera petición.
 */
public interface RoleResolver {

    /**
     * @param user usuario del ID token, con el rol que trae su custom claim
     * @return el rol guardado para ese usuario
     */
    Rol resolve(AuthenticatedUser user);
}
