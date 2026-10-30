package com.benchify.usuario.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Rol;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Perfil de un usuario de Benchify. El {@code id} es el UID de Firebase.
 * Inmutable: cada cambio devuelve una nueva instancia.
 */
public record Usuario(
        String id,
        String email,
        String nombre,
        String avatarUrl,
        String ciudad,
        Rol rol,
        Instant fechaRegistro,
        Instant ultimoAcceso) {

    public static final int MAX_NOMBRE = 100;
    public static final int MAX_CIUDAD = 100;

    /** Cada cuánto se guarda {@link #ultimoAcceso} aunque no haya cambiado nada más. */
    public static final Duration REFRESCO_ULTIMO_ACCESO = Duration.ofMinutes(15);

    public Usuario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(fechaRegistro, "fechaRegistro");
        Objects.requireNonNull(ultimoAcceso, "ultimoAcceso");
    }

    /**
     * Alta de un usuario que inicia sesión por primera vez. Es el único momento en que se toma el
     * rol del token: así se da de alta el primer administrador.
     */
    public static Usuario registrar(IdentidadUsuario identidad, Instant ahora) {
        return new Usuario(identidad.id(), identidad.email(), nombreVisible(identidad), identidad.avatarUrl(),
                null, identidad.rol(), ahora, ahora);
    }

    /**
     * Sincroniza los datos que vienen del proveedor de identidad. Ni el nombre (una vez registrado
     * lo gestiona el usuario desde su perfil) ni el rol (la base de datos es la fuente de verdad y
     * un token antiguo no puede cambiarlo) se tocan.
     */
    public Usuario sincronizar(IdentidadUsuario identidad, Instant ahora) {
        String avatar = identidad.avatarUrl() != null ? identidad.avatarUrl() : avatarUrl;
        return new Usuario(id, identidad.email(), nombre, avatar, ciudad, rol, fechaRegistro, ahora);
    }

    /** {@code true} si la identidad trae cambios que merece la pena persistir (el rol no cuenta). */
    public boolean difiereDe(IdentidadUsuario identidad) {
        return !Objects.equals(email, identidad.email())
                || (identidad.avatarUrl() != null && !identidad.avatarUrl().equals(avatarUrl));
    }

    /**
     * {@code true} si hay que escribir al sincronizar: la identidad trae cambios o el último acceso
     * guardado tiene más de {@link #REFRESCO_ULTIMO_ACCESO}.
     */
    public boolean debeSincronizarse(IdentidadUsuario identidad, Instant ahora) {
        return difiereDe(identidad) || ultimoAcceso.isBefore(ahora.minus(REFRESCO_ULTIMO_ACCESO));
    }

    public Usuario actualizarPerfil(String nuevoNombre, String nuevaCiudad) {
        String nombreLimpio = nuevoNombre == null ? "" : nuevoNombre.strip();
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > MAX_NOMBRE) {
            throw new BusinessRuleException("USUARIO_NOMBRE_INVALIDO",
                    "El nombre es obligatorio y no puede superar " + MAX_NOMBRE + " caracteres");
        }
        String ciudadLimpia = nuevaCiudad == null || nuevaCiudad.isBlank() ? null : nuevaCiudad.strip();
        if (ciudadLimpia != null && ciudadLimpia.length() > MAX_CIUDAD) {
            throw new BusinessRuleException("USUARIO_CIUDAD_INVALIDA",
                    "La ciudad no puede superar " + MAX_CIUDAD + " caracteres");
        }
        return new Usuario(id, email, nombreLimpio, avatarUrl, ciudadLimpia, rol, fechaRegistro, ultimoAcceso);
    }

    public Usuario conRol(Rol nuevoRol) {
        return new Usuario(id, email, nombre, avatarUrl, ciudad, nuevoRol, fechaRegistro, ultimoAcceso);
    }

    private static String nombreVisible(IdentidadUsuario identidad) {
        if (identidad.nombre() != null && !identidad.nombre().isBlank()) {
            return truncar(identidad.nombre().strip());
        }
        if (identidad.email() != null && identidad.email().contains("@")) {
            return truncar(identidad.email().substring(0, identidad.email().indexOf('@')));
        }
        return "Usuario";
    }

    private static String truncar(String value) {
        return value.length() > MAX_NOMBRE ? value.substring(0, MAX_NOMBRE) : value;
    }
}
