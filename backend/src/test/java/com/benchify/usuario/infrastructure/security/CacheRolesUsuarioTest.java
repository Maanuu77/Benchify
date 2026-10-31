package com.benchify.usuario.infrastructure.security;

import com.benchify.shared.domain.Rol;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class CacheRolesUsuarioTest {

    private final AtomicLong nanos = new AtomicLong();
    private final CacheRolesUsuario cache = new CacheRolesUsuario(nanos::get);
    private final AtomicInteger cargas = new AtomicInteger();

    @Test
    void loadsOnceAndRemembersTheRoleUntilItExpires() {
        assertThat(cache.obtener("ana", cargar(Rol.ADMIN))).isEqualTo(Rol.ADMIN);
        assertThat(cache.obtener("ana", cargar(Rol.USER))).isEqualTo(Rol.ADMIN);
        assertThat(cargas).hasValue(1);

        avanzar(CacheRolesUsuario.TTL.plusSeconds(1));

        assertThat(cache.obtener("ana", cargar(Rol.USER))).isEqualTo(Rol.USER);
        assertThat(cargas).hasValue(2);
    }

    @Test
    void invalidationAppliesOnTheNextRequest() {
        cache.obtener("ana", cargar(Rol.ADMIN));

        cache.invalidar("ana");

        assertThat(cache.obtener("ana", cargar(Rol.USER))).isEqualTo(Rol.USER);
    }

    @Test
    void invalidationOnlyAffectsThatUser() {
        cache.obtener("ana", cargar(Rol.ADMIN));
        cache.obtener("luis", cargar(Rol.ADMIN));

        cache.invalidar("ana");

        assertThat(cache.obtener("luis", cargar(Rol.USER))).isEqualTo(Rol.ADMIN);
    }

    @Test
    void aRoleReadBeforeAConcurrentChangeIsNotCached() {
        // La carga lee ADMIN y, antes de que termine, otro hilo confirma la degradación e invalida
        Rol leido = cache.obtener("ana", () -> {
            cache.invalidar("ana");
            return Rol.ADMIN;
        });

        assertThat(leido).isEqualTo(Rol.ADMIN);
        assertThat(cache.obtener("ana", cargar(Rol.USER))).isEqualTo(Rol.USER);
    }

    private Supplier<Rol> cargar(Rol rol) {
        return () -> {
            cargas.incrementAndGet();
            return rol;
        };
    }

    private void avanzar(Duration duracion) {
        nanos.addAndGet(duracion.toNanos());
    }
}
