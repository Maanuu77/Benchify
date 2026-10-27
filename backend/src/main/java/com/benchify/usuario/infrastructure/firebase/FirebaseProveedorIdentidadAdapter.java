package com.benchify.usuario.infrastructure.firebase;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.firebase.FirebaseIdTokenVerifier;
import com.benchify.usuario.application.port.out.ProveedorIdentidadPort;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** Guarda el rol como custom claim; el usuario lo verá en su próximo ID token. */
@Component
@ConditionalOnBooleanProperty(name = "benchify.firebase.enabled", matchIfMissing = true)
class FirebaseProveedorIdentidadAdapter implements ProveedorIdentidadPort {

    private final FirebaseAuth firebaseAuth;

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
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException("Could not update role in Firebase for user " + usuarioId, e);
        }
    }
}
