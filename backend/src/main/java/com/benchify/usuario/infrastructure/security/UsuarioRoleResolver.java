package com.benchify.usuario.infrastructure.security;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.shared.infrastructure.security.RoleResolver;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import org.springframework.stereotype.Component;

/**
 * Rol con el que se autoriza cada petición autenticada: el guardado en la base de datos. Al no
 * tenerlo en caché, sincroniza al usuario, lo que además garantiza que tiene perfil local antes de
 * llegar a un controlador (las demás features lo referencian por clave foránea) y le da de alta en
 * su primera petición.
 */
@Component
class UsuarioRoleResolver implements RoleResolver {

    private final SincronizarUsuarioUseCase sincronizarUsuario;
    private final CacheRolesUsuario cache;

    UsuarioRoleResolver(SincronizarUsuarioUseCase sincronizarUsuario, CacheRolesUsuario cache) {
        this.sincronizarUsuario = sincronizarUsuario;
        this.cache = cache;
    }

    @Override
    public Rol resolve(AuthenticatedUser user) {
        return cache.obtener(user.uid(), () -> sincronizarUsuario.sincronizar(new IdentidadUsuario(
                user.uid(), user.email(), user.name(), user.picture(), user.rol())).rol());
    }
}
