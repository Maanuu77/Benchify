package com.benchify.usuario.application.port.out;

/**
 * Actividad privada de un usuario que se borra al darse de baja: favoritos e historial de
 * check-ins. Lo publicado (bancos, reseñas, fotos) no se toca: se queda sin datos personales.
 */
public interface ActividadPersonalPort {

    void borrar(String usuarioId);
}
