package com.benchify.usuario.infrastructure.web;

import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.shared.infrastructure.security.FirebaseAuthentication;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Garantiza que todo usuario autenticado tiene perfil local antes de llegar a un controlador
 * (las demás features lo referencian por clave foránea). Recuerda durante unos minutos los
 * usuarios ya sincronizados para no consultar la base de datos en cada petición.
 */
class UsuarioSyncInterceptor implements HandlerInterceptor {

    static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_ENTRIES = 10_000;

    private final SincronizarUsuarioUseCase sincronizarUsuario;
    private final Clock clock;
    private final Map<String, Instant> sincronizados = new ConcurrentHashMap<>();

    UsuarioSyncInterceptor(SincronizarUsuarioUseCase sincronizarUsuario, Clock clock) {
        this.sincronizarUsuario = sincronizarUsuario;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof FirebaseAuthentication firebase) {
            AuthenticatedUser user = firebase.getPrincipal();
            String clave = user.uid() + '|' + user.rol() + '|' + user.email();
            Instant ahora = clock.instant();
            Instant ultimo = sincronizados.get(clave);
            if (ultimo == null || ultimo.plus(TTL).isBefore(ahora)) {
                sincronizarUsuario.sincronizar(new IdentidadUsuario(
                        user.uid(), user.email(), user.name(), user.picture(), user.rol()));
                if (sincronizados.size() >= MAX_ENTRIES) {
                    sincronizados.clear();
                }
                sincronizados.put(clave, ahora);
            }
        }
        return true;
    }
}
