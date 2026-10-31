package com.benchify.usuario.application.port.out;

import com.benchify.shared.domain.Rol;

/**
 * Operaciones sobre el proveedor de identidad externo (Firebase Authentication). El rol que
 * publica es un reflejo del de la base de datos para el cliente: la API no autoriza con él.
 */
public interface ProveedorIdentidadPort {

    /** Publica el rol como custom claim; el usuario lo verá en su próximo ID token. */
    void asignarRol(String usuarioId, Rol rol);

    /**
     * Como {@link #asignarRol}, para un usuario que ha llegado con un token cuyo rol no coincide
     * con el guardado. El adaptador puede omitir la escritura si acaba de publicar ese mismo rol:
     * un cliente con un token antiguo lo pediría en cada sincronización hasta que lo renueve.
     */
    void corregirRol(String usuarioId, Rol rol);

    /** Revoca los refresh tokens: el usuario tendrá que volver a iniciar sesión cuando caduque su ID token. */
    void revocarSesiones(String usuarioId);
}
