package com.benchify.usuario.infrastructure.web;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.shared.infrastructure.security.FirebaseAuthentication;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UsuarioSyncInterceptorTest {

    private final SincronizarUsuarioUseCase sincronizar = mock(SincronizarUsuarioUseCase.class);
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-25T10:00:00Z"));
    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };
    private final UsuarioSyncInterceptor interceptor = new UsuarioSyncInterceptor(sincronizar, clock);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousRequestsAreNotSynced() {
        interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        verifyNoInteractions(sincronizar);
    }

    @Test
    void syncsOncePerTtlWindow() {
        authenticate(Rol.USER);

        handle();
        handle();
        verify(sincronizar, times(1)).sincronizar(
                new IdentidadUsuario("ana", "ana@x.com", "Ana", null, Rol.USER));

        now.set(now.get().plus(UsuarioSyncInterceptor.TTL).plusSeconds(1));
        handle();
        verify(sincronizar, times(2)).sincronizar(
                new IdentidadUsuario("ana", "ana@x.com", "Ana", null, Rol.USER));
    }

    @Test
    void roleChangeInTheTokenTriggersASyncImmediately() {
        authenticate(Rol.USER);
        handle();
        authenticate(Rol.ADMIN);
        handle();

        verify(sincronizar).sincronizar(new IdentidadUsuario("ana", "ana@x.com", "Ana", null, Rol.ADMIN));
    }

    private void handle() {
        interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());
    }

    private static void authenticate(Rol rol) {
        SecurityContextHolder.getContext().setAuthentication(new FirebaseAuthentication(
                new AuthenticatedUser("ana", "ana@x.com", "Ana", null, true, rol)));
    }
}
