package com.benchify.usuario.infrastructure.firebase;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.firebase.FirebaseIdTokenVerifier;
import com.benchify.usuario.application.port.out.ProveedorIdentidadPort;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Guarda el rol como custom claim; el usuario lo verá en su próximo ID token. */
@Component
@ConditionalOnBooleanProperty(name = "benchify.firebase.enabled", matchIfMissing = true)
class FirebaseProveedorIdentidadAdapter implements ProveedorIdentidadPort {

    /** Lo que puede tardar un cliente en cambiar de ID token (es lo que dura uno). */
    static final Duration VIGENCIA_ID_TOKEN = Duration.ofHours(1);
    private static final int MAX_PUBLICADOS = 10_000;

    private final FirebaseAuth firebaseAuth;
    /** Último rol publicado por usuario, para no repetir la escritura mientras su token sea el antiguo. */
    private final Cache<String, Rol> publicados = Caffeine.newBuilder()
            .expireAfterWrite(VIGENCIA_ID_TOKEN)
            .maximumSize(MAX_PUBLICADOS)
            .build();

    FirebaseProveedorIdentidadAdapter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public void asignarRol(String usuarioId, Rol rol) {
        try {
            UserRecord user = firebaseAuth.getUser(usuarioId);
            Map<String, Object> claims = new HashMap<>(user.getCustomClaims());
            claims.put(FirebaseIdTokenVerifier.ROLE_CLAIM, rol.name());
            firebaseAuth.setCustomUserClaims(usuarioId, claims);
            publicados.put(usuarioId, rol);
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException("Could not update role in Firebase for user " + usuarioId, e);
        }
    }

    @Override
    public void corregirRol(String usuarioId, Rol rol) {
        if (publicados.getIfPresent(usuarioId) != rol) {
            asignarRol(usuarioId, rol);
        }
    }

    @Override
    public void revocarSesiones(String usuarioId) {
        try {
            firebaseAuth.revokeRefreshTokens(usuarioId);
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException("Could not revoke Firebase sessions of user " + usuarioId, e);
        }
    }
}
