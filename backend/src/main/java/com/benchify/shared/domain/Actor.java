package com.benchify.shared.domain;

import java.util.Objects;

/**
 * Quién ejecuta un caso de uso. Los servicios de aplicación reciben un {@code Actor}
 * en lugar de depender de Spring Security.
 *
 * @param userId          UID de Firebase del usuario autenticado
 * @param emailVerificado si ha confirmado su email; sin confirmarlo no puede publicar contenido
 */
public record Actor(String userId, Rol rol, boolean emailVerificado) {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(rol, "rol");
    }

    /** Actor con el email ya verificado (usuarios de Google, tests, procesos internos). */
    public Actor(String userId, Rol rol) {
        this(userId, rol, true);
    }

    public static Actor user(String userId) {
        return new Actor(userId, Rol.USER);
    }

    public static Actor admin(String userId) {
        return new Actor(userId, Rol.ADMIN);
    }

    public boolean isAdmin() {
        return rol == Rol.ADMIN;
    }

    /**
     * Publicar contenido (bancos, reseñas, fotos) exige haber verificado el email: frena las cuentas
     * desechables. Los administradores quedan exentos.
     */
    public void exigirEmailVerificado() {
        if (!emailVerificado && !isAdmin()) {
            throw new ForbiddenException("EMAIL_NO_VERIFICADO", "Verifica tu email para publicar contenido");
        }
    }

    /** {@code true} si el actor es el propietario del recurso o un administrador. */
    public boolean canManage(String ownerId) {
        return isAdmin() || userId.equals(ownerId);
    }
}
