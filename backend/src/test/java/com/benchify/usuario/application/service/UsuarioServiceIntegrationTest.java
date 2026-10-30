package com.benchify.usuario.application.service;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Rol;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.RecordingIdentityProvider;
import com.benchify.usuario.application.port.in.AdministrarUsuariosUseCase;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de uso de usuarios contra PostgreSQL. Sin {@code @Transactional}: cada llamada confirma
 * su transacción, como en producción. Los UID son únicos por test.
 */
class UsuarioServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SincronizarUsuarioUseCase sincronizarUsuario;
    @Autowired
    private AdministrarUsuariosUseCase administrarUsuarios;
    @Autowired
    private RecordingIdentityProvider proveedorIdentidad;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;

    private String uid;

    @BeforeEach
    void nuevoUsuario() {
        uid = "it-" + UUID.randomUUID();
    }

    @Test
    void lastAccessIsRefreshedWhenOlderThanFifteenMinutes() {
        sincronizarUsuario.sincronizar(identidad(uid, Rol.USER));
        jdbc.update("UPDATE usuarios SET ultimo_acceso = now() - interval '20 minutes' WHERE id = ?", uid);
        Instant antes = Instant.now().minusSeconds(60);

        sincronizarUsuario.sincronizar(identidad(uid, Rol.USER));

        assertThat(ultimoAcceso()).isAfter(antes);
    }

    @Test
    void recentLastAccessIsNotRewritten() {
        sincronizarUsuario.sincronizar(identidad(uid, Rol.USER));
        jdbc.update("UPDATE usuarios SET ultimo_acceso = now() - interval '5 minutes' WHERE id = ?", uid);
        Instant guardado = ultimoAcceso();

        sincronizarUsuario.sincronizar(identidad(uid, Rol.USER));

        assertThat(ultimoAcceso()).isEqualTo(guardado);
        assertThat(Duration.between(guardado, Instant.now())).isGreaterThan(Duration.ofMinutes(4));
    }

    @Test
    void staleAdminTokenDoesNotRewriteTheStoredRole() {
        haciendoFaltaUnAdministrador();
        sincronizarUsuario.sincronizar(identidad(uid, Rol.USER));

        Usuario sincronizado = sincronizarUsuario.sincronizar(identidad(uid, Rol.ADMIN));

        assertThat(sincronizado.rol()).isEqualTo(Rol.USER);
        assertThat(rolGuardado()).isEqualTo("USER");
        assertThat(proveedorIdentidad.llamadas(uid)).containsExactly("corregirRol:USER");
    }

    @Test
    void firstAdministratorIsBootstrappedFromTheClaimWhileThereIsNone() {
        sincronizarUsuario.sincronizar(identidad(uid, Rol.USER));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            // Base de datos sin administradores; la transacción se deshace al final para no afectar a otros tests
            jdbc.update("UPDATE usuarios SET rol = 'USER' WHERE rol = 'ADMIN'");

            assertThat(sincronizarUsuario.sincronizar(identidad(uid, Rol.ADMIN)).rol()).isEqualTo(Rol.ADMIN);
            assertThat(rolGuardado()).isEqualTo("ADMIN");
            status.setRollbackOnly();
        });

        assertThat(rolGuardado()).isEqualTo("USER");
    }

    @Test
    void roleChangeReachesTheIdentityProviderAfterCommit() {
        String admin = haciendoFaltaUnAdministrador();
        sincronizarUsuario.sincronizar(identidad(uid, Rol.USER));

        administrarUsuarios.cambiarRol(Actor.admin(admin), uid, Rol.ADMIN);

        assertThat(rolGuardado()).isEqualTo("ADMIN");
        assertThat(proveedorIdentidad.llamadas(uid)).containsExactly("asignarRol:ADMIN", "revocarSesiones");
    }

    @Test
    void rolledBackRoleChangeNeverReachesTheIdentityProvider() {
        String admin = haciendoFaltaUnAdministrador();
        sincronizarUsuario.sincronizar(identidad(uid, Rol.USER));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            administrarUsuarios.cambiarRol(Actor.admin(admin), uid, Rol.ADMIN);
            status.setRollbackOnly();
        });

        assertThat(rolGuardado()).isEqualTo("USER");
        assertThat(proveedorIdentidad.llamadas(uid)).isEmpty();
    }

    /** Da de alta a un administrador (el claim cuenta en el alta) y devuelve su UID. */
    private String haciendoFaltaUnAdministrador() {
        String admin = "it-admin-" + UUID.randomUUID();
        assertThat(sincronizarUsuario.sincronizar(identidad(admin, Rol.ADMIN)).rol()).isEqualTo(Rol.ADMIN);
        return admin;
    }

    private static IdentidadUsuario identidad(String id, Rol rolEnElToken) {
        return new IdentidadUsuario(id, id + "@benchify.test", "Usuario " + id, null, rolEnElToken);
    }

    private Instant ultimoAcceso() {
        return jdbc.queryForObject("SELECT ultimo_acceso FROM usuarios WHERE id = ?", Timestamp.class, uid)
                .toInstant();
    }

    private String rolGuardado() {
        return jdbc.queryForObject("SELECT rol FROM usuarios WHERE id = ?", String.class, uid);
    }
}
