package com.benchify.usuario.application.service;

import com.benchify.shared.domain.Rol;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

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
    private JdbcTemplate jdbc;

    private String uid;

    @BeforeEach
    void nuevoUsuario() {
        uid = "it-" + UUID.randomUUID();
    }

    @Test
    void lastAccessIsRefreshedWhenOlderThanFifteenMinutes() {
        sincronizarUsuario.sincronizar(identidad(Rol.USER));
        jdbc.update("UPDATE usuarios SET ultimo_acceso = now() - interval '20 minutes' WHERE id = ?", uid);
        Instant antes = Instant.now().minusSeconds(60);

        sincronizarUsuario.sincronizar(identidad(Rol.USER));

        assertThat(ultimoAcceso()).isAfter(antes);
    }

    @Test
    void recentLastAccessIsNotRewritten() {
        sincronizarUsuario.sincronizar(identidad(Rol.USER));
        jdbc.update("UPDATE usuarios SET ultimo_acceso = now() - interval '5 minutes' WHERE id = ?", uid);
        Instant guardado = ultimoAcceso();

        sincronizarUsuario.sincronizar(identidad(Rol.USER));

        assertThat(ultimoAcceso()).isEqualTo(guardado);
        assertThat(Duration.between(guardado, Instant.now())).isGreaterThan(Duration.ofMinutes(4));
    }

    private IdentidadUsuario identidad(Rol rolEnElToken) {
        return new IdentidadUsuario(uid, uid + "@benchify.test", "Usuario " + uid, null, rolEnElToken);
    }

    private Instant ultimoAcceso() {
        return jdbc.queryForObject("SELECT ultimo_acceso FROM usuarios WHERE id = ?", Timestamp.class, uid)
                .toInstant();
    }
}
