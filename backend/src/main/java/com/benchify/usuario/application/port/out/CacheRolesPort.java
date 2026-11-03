package com.benchify.usuario.application.port.out;

/**
 * Caché local del rol con el que se autoriza a cada usuario. Un cambio de rol la invalida para
 * que se aplique en la siguiente petición sin esperar a que caduque.
 */
public interface CacheRolesPort {

    void invalidar(String usuarioId);
}
