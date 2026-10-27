package com.benchify.usuario.application.port.out;

import com.benchify.usuario.domain.EstadisticasUsuario;

/**
 * Modelo de lectura que agrega datos de varias features. Se resuelve con una consulta sobre
 * sus tablas en lugar de llamar a cada feature, para no hacer cuatro idas y vueltas.
 */
public interface EstadisticasUsuarioPort {

    EstadisticasUsuario contar(String usuarioId);
}
