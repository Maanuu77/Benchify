package com.benchify.usuario.infrastructure.persistence;

import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import com.benchify.usuario.domain.UsuarioNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UsuarioPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private UsuarioRepository repository;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void savesAndReadsBackAUser() {
        Usuario usuario = Usuario.registrar(
                new IdentidadUsuario("uid-persist", "ana@example.com", "Ana", "https://img/a.png", Rol.ADMIN), AHORA);

        repository.registrarSiNoExiste(usuario);

        assertThat(repository.existsById("uid-persist")).isTrue();
        assertThat(repository.findById("uid-persist")).contains(usuario);
    }

    @Test
    void registersOnlyOnceAndReturnsTheStoredRow() {
        Usuario ana = Usuario.registrar(
                new IdentidadUsuario("uid-upsert", "ana@example.com", "Ana", null, Rol.ADMIN), AHORA);
        Usuario otra = Usuario.registrar(
                new IdentidadUsuario("uid-upsert", "otra@example.com", "Otra", "https://img/o.png", Rol.USER),
                AHORA.plusSeconds(60));

        assertThat(repository.registrarSiNoExiste(ana)).isEqualTo(ana);
        // La segunda alta no falla ni pisa nada: devuelve la fila que ya estaba
        assertThat(repository.registrarSiNoExiste(otra)).isEqualTo(ana);
        assertThat(repository.findById("uid-upsert")).contains(ana);
    }

    @Test
    void updatesWriteOnlyTheirOwnColumns() {
        Usuario leido = repository.registrarSiNoExiste(Usuario.registrar(
                new IdentidadUsuario("uid-columnas", "ana@example.com", "Ana", null, Rol.USER), AHORA));

        repository.actualizarRol("uid-columnas", Rol.ADMIN);
        // Copias leídas antes del cambio de rol: ni la sincronización ni el perfil lo deshacen
        repository.actualizarSincronizacion(leido.sincronizar(
                new IdentidadUsuario("uid-columnas", "nuevo@example.com", "Ana", "https://img/n.png", Rol.USER),
                AHORA.plusSeconds(60)));
        repository.actualizarPerfil(leido.actualizarPerfil("Ana María", "Sevilla"));

        assertThat(repository.findById("uid-columnas")).hasValueSatisfying(usuario -> {
            assertThat(usuario.rol()).isEqualTo(Rol.ADMIN);
            assertThat(usuario.email()).isEqualTo("nuevo@example.com");
            assertThat(usuario.avatarUrl()).isEqualTo("https://img/n.png");
            assertThat(usuario.ultimoAcceso()).isEqualTo(AHORA.plusSeconds(60));
            assertThat(usuario.nombre()).isEqualTo("Ana María");
            assertThat(usuario.ciudad()).isEqualTo("Sevilla");
            assertThat(usuario.fechaRegistro()).isEqualTo(AHORA);
        });
    }

    @Test
    void updatingAMissingUserFails() {
        assertThatThrownBy(() -> repository.actualizarRol("uid-inexistente", Rol.ADMIN))
                .isInstanceOf(UsuarioNotFoundException.class);
    }

    @Test
    void knowsWhetherThereIsAnyAdministrator() {
        repository.registrarSiNoExiste(Usuario.registrar(
                new IdentidadUsuario("uid-admin", null, "Admin", null, Rol.ADMIN), AHORA));
        assertThat(repository.existeAdministrador()).isTrue();

        // Dentro de la transacción del test, que se deshace al terminar
        jdbc.update("UPDATE usuarios SET rol = 'USER' WHERE rol = 'ADMIN'");
        assertThat(repository.existeAdministrador()).isFalse();
    }

    @Test
    void pagesUsersFromNewestToOldest() {
        repository.registrarSiNoExiste(Usuario.registrar(
                new IdentidadUsuario("old", null, "Old", null, Rol.USER), AHORA.minusSeconds(60)));
        repository.registrarSiNoExiste(Usuario.registrar(
                new IdentidadUsuario("new", null, "New", null, Rol.USER), AHORA));

        PageResult<Usuario> page = repository.findAll(PageQuery.first(1));

        assertThat(page.content()).extracting(Usuario::id).containsExactly("new");
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(2);
    }
}
