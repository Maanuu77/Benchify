package com.benchify.usuario.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Rol;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");

    @Test
    void registersWithTheNameFromTheIdentityProvider() {
        Usuario usuario = Usuario.registrar(
                new IdentidadUsuario("uid", "ana@example.com", " Ana López ", null, Rol.USER), AHORA);

        assertThat(usuario.nombre()).isEqualTo("Ana López");
        assertThat(usuario.fechaRegistro()).isEqualTo(AHORA);
        assertThat(usuario.ultimoAcceso()).isEqualTo(AHORA);
        assertThat(usuario.rol()).isEqualTo(Rol.USER);
    }

    @Test
    void fallsBackToEmailLocalPartWhenThereIsNoName() {
        Usuario usuario = Usuario.registrar(
                new IdentidadUsuario("uid", "luis.perez@example.com", "  ", null, Rol.USER), AHORA);

        assertThat(usuario.nombre()).isEqualTo("luis.perez");
    }

    @Test
    void fallsBackToGenericNameWithoutNameOrEmail() {
        Usuario usuario = Usuario.registrar(new IdentidadUsuario("uid", null, null, null, Rol.USER), AHORA);

        assertThat(usuario.nombre()).isEqualTo("Usuario");
    }

    @Test
    void syncUpdatesRoleAndEmailButKeepsTheChosenName() {
        Usuario usuario = Usuario.registrar(new IdentidadUsuario("uid", "a@x.com", "Ana", null, Rol.USER), AHORA)
                .actualizarPerfil("Anita", "Sevilla");
        IdentidadUsuario identidad = new IdentidadUsuario("uid", "b@x.com", "Ana", null, Rol.ADMIN);

        assertThat(usuario.difiereDe(identidad)).isTrue();
        Usuario sincronizado = usuario.sincronizar(identidad, AHORA.plusSeconds(60));

        assertThat(sincronizado.nombre()).isEqualTo("Anita");
        assertThat(sincronizado.ciudad()).isEqualTo("Sevilla");
        assertThat(sincronizado.email()).isEqualTo("b@x.com");
        assertThat(sincronizado.rol()).isEqualTo(Rol.ADMIN);
        assertThat(sincronizado.ultimoAcceso()).isEqualTo(AHORA.plusSeconds(60));
        assertThat(sincronizado.fechaRegistro()).isEqualTo(AHORA);
    }

    @Test
    void identicalIdentityNeedsNoSync() {
        IdentidadUsuario identidad = new IdentidadUsuario("uid", "a@x.com", "Ana", "https://img/a.png", Rol.USER);
        Usuario usuario = Usuario.registrar(identidad, AHORA);

        assertThat(usuario.difiereDe(identidad)).isFalse();
        assertThat(usuario.difiereDe(new IdentidadUsuario("uid", "a@x.com", "Ana", null, Rol.USER))).isFalse();
    }

    @Test
    void profileUpdateTrimsValuesAndClearsBlankCity() {
        Usuario usuario = Usuario.registrar(new IdentidadUsuario("uid", null, "Ana", null, Rol.USER), AHORA)
                .actualizarPerfil("  Ana María ", "   ");

        assertThat(usuario.nombre()).isEqualTo("Ana María");
        assertThat(usuario.ciudad()).isNull();
    }

    @Test
    void profileUpdateRejectsBlankOrTooLongNames() {
        Usuario usuario = Usuario.registrar(new IdentidadUsuario("uid", null, "Ana", null, Rol.USER), AHORA);

        assertThatThrownBy(() -> usuario.actualizarPerfil(" ", null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("USUARIO_NOMBRE_INVALIDO");
        assertThatThrownBy(() -> usuario.actualizarPerfil("x".repeat(101), null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> usuario.actualizarPerfil("Ana", "x".repeat(101)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("USUARIO_CIUDAD_INVALIDA");
    }
}
