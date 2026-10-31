package com.benchify.usuario.infrastructure.security;

import com.benchify.shared.domain.Rol;
import com.benchify.usuario.application.port.out.CacheRolesPort;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Rol de cada usuario recordado unos segundos para no ir a la base de datos en cada petición. Es
 * local a la instancia: un cambio de rol la invalida aquí en cuanto se confirma y en las demás
 * instancias se aplica al caducar la entrada ({@link #TTL}).
 */
@Component
class CacheRolesUsuario implements CacheRolesPort {

    static final Duration TTL = Duration.ofSeconds(30);
    static final int MAX_ENTRADAS = 10_000;

    private final Cache<String, Rol> roles;
    /** Cuenta las invalidaciones para descartar un rol que se leyó antes de un cambio aún en curso. */
    private final AtomicLong invalidaciones = new AtomicLong();

    CacheRolesUsuario() {
        this(Ticker.systemTicker());
    }

    CacheRolesUsuario(Ticker ticker) {
        this.roles = Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_ENTRADAS)
                .ticker(ticker)
                .build();
    }

    /**
     * Devuelve el rol recordado o lo carga. La carga no bloquea la caché (puede tocar la base de
     * datos y Firebase), así que dos peticiones simultáneas del mismo usuario pueden cargarlo a la vez.
     */
    Rol obtener(String usuarioId, Supplier<Rol> cargar) {
        Rol rol = roles.getIfPresent(usuarioId);
        if (rol != null) {
            return rol;
        }
        long version = invalidaciones.get();
        Rol cargado = cargar.get();
        // Si alguien ha invalidado mientras se cargaba, lo leído puede ser anterior a su cambio: no se guarda.
        // compute() y la invalidación se excluyen sobre la misma clave, así que no hay hueco entre la
        // comprobación y la escritura.
        roles.asMap().compute(usuarioId, (id, actual) -> invalidaciones.get() == version ? cargado : actual);
        return cargado;
    }

    @Override
    public void invalidar(String usuarioId) {
        invalidaciones.incrementAndGet();
        roles.invalidate(usuarioId);
    }
}
