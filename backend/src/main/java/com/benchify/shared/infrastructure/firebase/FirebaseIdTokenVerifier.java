package com.benchify.shared.infrastructure.firebase;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.shared.infrastructure.security.IdTokenVerifier;
import com.benchify.shared.infrastructure.security.InvalidIdTokenException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

/** Verifica la firma, la caducidad y la audiencia del ID token con el Admin SDK. */
public class FirebaseIdTokenVerifier implements IdTokenVerifier {

    /** Custom claim con el rol, asignado con {@code FirebaseAuth#setCustomUserClaims}. */
    public static final String ROLE_CLAIM = "role";

    private final FirebaseAuth firebaseAuth;

    public FirebaseIdTokenVerifier(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public AuthenticatedUser verify(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            return new AuthenticatedUser(
                    token.getUid(),
                    token.getEmail(),
                    token.getName(),
                    token.getPicture(),
                    token.isEmailVerified(),
                    Rol.fromClaim(token.getClaims().get(ROLE_CLAIM)));
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            throw new InvalidIdTokenException("Invalid Firebase ID token", e);
        }
    }
}
