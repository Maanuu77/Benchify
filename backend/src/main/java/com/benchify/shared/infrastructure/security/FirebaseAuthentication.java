package com.benchify.shared.infrastructure.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/** Autenticación de Spring Security respaldada por un ID token ya verificado. */
public class FirebaseAuthentication extends AbstractAuthenticationToken {

    private final AuthenticatedUser user;

    public FirebaseAuthentication(AuthenticatedUser user) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + user.rol().name())));
        this.user = user;
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return user;
    }

    /** El token no se conserva una vez verificado. */
    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public String getName() {
        return user.uid();
    }
}
