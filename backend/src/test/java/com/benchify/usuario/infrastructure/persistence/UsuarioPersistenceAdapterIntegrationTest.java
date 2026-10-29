package com.benchify.usuario.infrastructure.persistence;

import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UsuarioPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private UsuarioRepository repository;

    @Test
    void savesAndReadsBackAUser() {
        Usuario usuario = Usuario.registrar(
                new IdentidadUsuario("uid-persist", "ana@example.com", "Ana", "https://img/a.png", Rol.ADMIN), AHORA)
                .actualizarPerfil("Ana", "Sevilla");

        repository.save(usuario);

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
    void pagesUsersFromNewestToOldest() {
        repository.save(Usuario.registrar(new IdentidadUsuario("old", null, "Old", null, Rol.USER), AHORA.minusSeconds(60)));
        repository.save(Usuario.registrar(new IdentidadUsuario("new", null, "New", null, Rol.USER), AHORA));

        PageResult<Usuario> page = repository.findAll(PageQuery.first(1));

        assertThat(page.content()).extracting(Usuario::id).containsExactly("new");
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(2);
    }
}
