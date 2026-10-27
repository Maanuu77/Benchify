package com.benchify.shared.infrastructure.firebase;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.shared.infrastructure.security.InvalidIdTokenException;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FirebaseIdTokenVerifierTest {

    private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    private final FirebaseIdTokenVerifier verifier = new FirebaseIdTokenVerifier(firebaseAuth);

    @Test
    void mapsTokenClaimsToAuthenticatedUser() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid-1");
        when(token.getEmail()).thenReturn("ana@example.com");
        when(token.getName()).thenReturn("Ana");
        when(token.getPicture()).thenReturn("https://example.com/ana.png");
        when(token.isEmailVerified()).thenReturn(true);
        when(token.getClaims()).thenReturn(Map.of("role", "ADMIN"));
        when(firebaseAuth.verifyIdToken("valid")).thenReturn(token);

        AuthenticatedUser user = verifier.verify("valid");

        assertThat(user).isEqualTo(new AuthenticatedUser("uid-1", "ana@example.com", "Ana",
                "https://example.com/ana.png", true, Rol.ADMIN));
    }

    @Test
    void usersWithoutRoleClaimAreRegularUsers() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid-2");
        when(token.getClaims()).thenReturn(Map.of());
        when(firebaseAuth.verifyIdToken("valid")).thenReturn(token);

        assertThat(verifier.verify("valid").rol()).isEqualTo(Rol.USER);
    }

    @Test
    void wrapsFirebaseErrorsAsInvalidToken() throws Exception {
        when(firebaseAuth.verifyIdToken("expired")).thenThrow(new FirebaseAuthException(
                ErrorCode.INVALID_ARGUMENT, "expired", null, null, AuthErrorCode.EXPIRED_ID_TOKEN));

        assertThatThrownBy(() -> verifier.verify("expired"))
                .isInstanceOf(InvalidIdTokenException.class)
                .hasCauseInstanceOf(FirebaseAuthException.class);
    }
}
