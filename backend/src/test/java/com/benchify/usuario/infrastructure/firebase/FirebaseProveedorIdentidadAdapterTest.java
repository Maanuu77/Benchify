package com.benchify.usuario.infrastructure.firebase;

import com.benchify.shared.domain.Rol;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirebaseProveedorIdentidadAdapterTest {

    private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    private final FirebaseProveedorIdentidadAdapter adapter = new FirebaseProveedorIdentidadAdapter(firebaseAuth);

    @Test
    void setsRoleClaimKeepingOtherCustomClaims() throws Exception {
        usuarioConClaims("ana", Map.of("beta", true, "role", "USER"));

        adapter.asignarRol("ana", Rol.ADMIN);

        verify(firebaseAuth).setCustomUserClaims("ana", Map.of("beta", true, "role", "ADMIN"));
    }

    @Test
    void wrapsFirebaseErrors() throws Exception {
        when(firebaseAuth.getUser("ghost")).thenThrow(new FirebaseAuthException(
                ErrorCode.NOT_FOUND, "no user", null, null, AuthErrorCode.USER_NOT_FOUND));

        assertThatThrownBy(() -> adapter.asignarRol("ghost", Rol.ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(FirebaseAuthException.class);
    }

    @Test
    void correctionIsSkippedWhileTheSameRoleWasJustPublished() throws Exception {
        usuarioConClaims("ana", Map.of("role", "ADMIN"));

        adapter.asignarRol("ana", Rol.USER);
        // Su token antiguo sigue diciendo ADMIN hasta que lo renueve: no se repite la escritura
        adapter.corregirRol("ana", Rol.USER);
        adapter.corregirRol("ana", Rol.USER);

        verify(firebaseAuth, times(1)).setCustomUserClaims(eq("ana"), anyMap());
    }

    @Test
    void correctionWritesWhenTheRoleWasNotPublishedFromThisInstance() throws Exception {
        usuarioConClaims("ana", Map.of("role", "ADMIN"));

        adapter.corregirRol("ana", Rol.USER);
        adapter.corregirRol("ana", Rol.USER);

        verify(firebaseAuth, times(1)).setCustomUserClaims("ana", Map.of("role", "USER"));
    }

    @Test
    void failedPublicationIsRetriedOnTheNextCorrection() throws Exception {
        usuarioConClaims("ana", Map.of("role", "ADMIN"));
        doThrow(new FirebaseAuthException(ErrorCode.UNAVAILABLE, "down", null, null, null))
                .doNothing()
                .when(firebaseAuth).setCustomUserClaims(eq("ana"), anyMap());

        assertThatThrownBy(() -> adapter.asignarRol("ana", Rol.USER)).isInstanceOf(IllegalStateException.class);
        adapter.corregirRol("ana", Rol.USER);

        verify(firebaseAuth, times(2)).setCustomUserClaims("ana", Map.of("role", "USER"));
    }

    @Test
    void revokesRefreshTokens() throws Exception {
        adapter.revocarSesiones("ana");

        verify(firebaseAuth).revokeRefreshTokens("ana");
    }

    @Test
    void wrapsRevocationErrors() throws Exception {
        doThrow(new FirebaseAuthException(ErrorCode.NOT_FOUND, "no user", null, null, AuthErrorCode.USER_NOT_FOUND))
                .when(firebaseAuth).revokeRefreshTokens("ghost");

        assertThatThrownBy(() -> adapter.revocarSesiones("ghost"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(FirebaseAuthException.class);
    }

    private void usuarioConClaims(String uid, Map<String, Object> claims) throws FirebaseAuthException {
        UserRecord user = mock(UserRecord.class);
        when(user.getCustomClaims()).thenReturn(claims);
        when(firebaseAuth.getUser(uid)).thenReturn(user);
    }
}
