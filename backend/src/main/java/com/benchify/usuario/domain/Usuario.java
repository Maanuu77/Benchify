package com.benchify.usuario.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Rol;

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

    public Usuario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(fechaRegistro, "fechaRegistro");
        Objects.requireNonNull(ultimoAcceso, "ultimoAcceso");
    }

    /** Alta de un usuario que inicia sesión por primera vez. */
    public static Usuario registrar(IdentidadUsuario identidad, Instant ahora) {
        return new Usuario(identidad.id(), identidad.email(), nombreVisible(identidad), identidad.avatarUrl(),
                null, identidad.rol(), ahora, ahora);
    }

    /**
     * Sincroniza los datos que vienen del proveedor de identidad. El nombre no se toca:
     * una vez registrado, el usuario lo gestiona desde su perfil.
     */
    public Usuario sincronizar(IdentidadUsuario identidad, Instant ahora) {
        String avatar = identidad.avatarUrl() != null ? identidad.avatarUrl() : avatarUrl;
        return new Usuario(id, identidad.email(), nombre, avatar, ciudad, identidad.rol(), fechaRegistro, ahora);
    }

    /** {@code true} si la identidad trae cambios que merece la pena persistir. */
    public boolean difiereDe(IdentidadUsuario identidad) {
        return rol != identidad.rol()
                || !Objects.equals(email, identidad.email())
                || (identidad.avatarUrl() != null && !identidad.avatarUrl().equals(avatarUrl));
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
