package com.benchify.usuario.infrastructure.firebase;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.firebase.FirebaseIdTokenVerifier;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Solo en desarrollo y contra el emulador: crea un administrador y un usuario con UID fijo
 * para poder iniciar sesión nada más levantar el entorno. Los datos de ejemplo de
 * {@code db/seed} referencian estos UID.
 */
@Component
@Profile("dev")
@ConditionalOnBooleanProperty(name = "benchify.seed.enabled")
class DevUsersSeeder {

    static final List<DevUser> USERS = List.of(
            new DevUser("dev-admin", "admin@benchify.dev", "Admin Benchify", Rol.ADMIN),
            new DevUser("dev-user", "ana@benchify.dev", "Ana García", Rol.USER));

    private static final Logger log = LoggerFactory.getLogger(DevUsersSeeder.class);

    private final FirebaseAuth firebaseAuth;
    private final SincronizarUsuarioUseCase sincronizarUsuario;
    private final String password;

    DevUsersSeeder(FirebaseAuth firebaseAuth, SincronizarUsuarioUseCase sincronizarUsuario,
            @Value("${benchify.seed.users-password}") String password) {
        this.firebaseAuth = firebaseAuth;
        this.sincronizarUsuario = sincronizarUsuario;
        this.password = password;
    }

    @EventListener(ApplicationReadyEvent.class)
    void seed() {
        for (DevUser user : USERS) {
            try {
                if (!exists(user.uid())) {
                    firebaseAuth.createUser(new UserRecord.CreateRequest()
                            .setUid(user.uid())
                            .setEmail(user.email())
                            .setEmailVerified(true)
                            .setPassword(password)
                            .setDisplayName(user.nombre()));
                    log.info("Created dev user {} ({}) in the Auth emulator", user.email(), user.rol());
                }
                firebaseAuth.setCustomUserClaims(user.uid(),
                        Map.of(FirebaseIdTokenVerifier.ROLE_CLAIM, user.rol().name()));
                sincronizarUsuario.sincronizar(
                        new IdentidadUsuario(user.uid(), user.email(), user.nombre(), null, user.rol()));
            } catch (FirebaseAuthException | RuntimeException e) {
                log.warn("Could not seed dev user {}: is the Auth emulator running? ({})", user.email(), e.getMessage());
                return;
            }
        }
    }

    private boolean exists(String uid) throws FirebaseAuthException {
        try {
            firebaseAuth.getUser(uid);
            return true;
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    record DevUser(String uid, String email, String nombre, Rol rol) {
    }
}
