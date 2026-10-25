package com.benchify.usuario.infrastructure.firebase;

import com.benchify.shared.domain.Rol;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevUsersSeederTest {

    private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    private final SincronizarUsuarioUseCase sincronizar = mock(SincronizarUsuarioUseCase.class);
    private final DevUsersSeeder seeder = new DevUsersSeeder(firebaseAuth, sincronizar, "secret-for-tests");

    @Test
    void createsMissingUsersWithRoleClaimsAndLocalProfile() throws Exception {
        when(firebaseAuth.getUser(any())).thenThrow(notFound());

        seeder.seed();

        verify(firebaseAuth, times(2)).createUser(any(UserRecord.CreateRequest.class));
        verify(firebaseAuth).setCustomUserClaims("dev-admin", Map.of("role", "ADMIN"));
        verify(firebaseAuth).setCustomUserClaims("dev-user", Map.of("role", "USER"));
        verify(sincronizar).sincronizar(
                new IdentidadUsuario("dev-admin", "admin@benchify.dev", "Admin Benchify", null, Rol.ADMIN));
    }

    @Test
    void doesNotRecreateExistingUsers() throws Exception {
        when(firebaseAuth.getUser(any())).thenReturn(mock(UserRecord.class));

        seeder.seed();

        verify(firebaseAuth, never()).createUser(any(UserRecord.CreateRequest.class));
        verify(firebaseAuth, times(2)).setCustomUserClaims(any(), any());
    }

    @Test
    void stopsQuietlyWhenTheEmulatorIsDown() throws Exception {
        when(firebaseAuth.getUser(any())).thenThrow(new IllegalStateException("connection refused"));

        seeder.seed();

        verify(firebaseAuth, times(1)).getUser(any());
        verify(sincronizar, never()).sincronizar(any());
    }

    private static FirebaseAuthException notFound() {
        return new FirebaseAuthException(ErrorCode.NOT_FOUND, "no user", null, null, AuthErrorCode.USER_NOT_FOUND);
    }
}
