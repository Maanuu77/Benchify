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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirebaseProveedorIdentidadAdapterTest {

    private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    private final FirebaseProveedorIdentidadAdapter adapter = new FirebaseProveedorIdentidadAdapter(firebaseAuth);

    @Test
    void setsRoleClaimKeepingOtherCustomClaims() throws Exception {
        UserRecord user = mock(UserRecord.class);
        when(user.getCustomClaims()).thenReturn(Map.of("beta", true, "role", "USER"));
        when(firebaseAuth.getUser("ana")).thenReturn(user);

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
}
