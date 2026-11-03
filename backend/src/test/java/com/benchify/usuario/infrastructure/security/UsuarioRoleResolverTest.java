package com.benchify.usuario.infrastructure.security;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioRoleResolverTest {

    private static final AuthenticatedUser ANA_CON_CLAIM_ADMIN =
            new AuthenticatedUser("ana", "ana@x.com", "Ana", "https://img/a.png", true, Rol.ADMIN);

    private final SincronizarUsuarioUseCase sincronizarUsuario = mock(SincronizarUsuarioUseCase.class);
    private final CacheRolesUsuario cache = new CacheRolesUsuario();
    private final UsuarioRoleResolver resolver = new UsuarioRoleResolver(sincronizarUsuario, cache);

    @Test
    void syncsTheUserAndReturnsTheStoredRole() {
        when(sincronizarUsuario.sincronizar(any())).thenReturn(usuario(Rol.USER));

        assertThat(resolver.resolve(ANA_CON_CLAIM_ADMIN)).isEqualTo(Rol.USER);
        // El rol del claim viaja hasta el caso de uso: solo cuenta si el usuario es nuevo
        verify(sincronizarUsuario).sincronizar(
                new IdentidadUsuario("ana", "ana@x.com", "Ana", "https://img/a.png", Rol.ADMIN));
    }

    @Test
    void cachesTheRoleUntilItIsInvalidated() {
        when(sincronizarUsuario.sincronizar(any())).thenReturn(usuario(Rol.ADMIN), usuario(Rol.USER));

        assertThat(resolver.resolve(ANA_CON_CLAIM_ADMIN)).isEqualTo(Rol.ADMIN);
        assertThat(resolver.resolve(ANA_CON_CLAIM_ADMIN)).isEqualTo(Rol.ADMIN);
        verify(sincronizarUsuario, times(1)).sincronizar(any());

        cache.invalidar("ana");

        assertThat(resolver.resolve(ANA_CON_CLAIM_ADMIN)).isEqualTo(Rol.USER);
        verify(sincronizarUsuario, times(2)).sincronizar(any());
    }

    private static Usuario usuario(Rol rol) {
        return Usuario.registrar(new IdentidadUsuario("ana", "ana@x.com", "Ana", null, rol),
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
