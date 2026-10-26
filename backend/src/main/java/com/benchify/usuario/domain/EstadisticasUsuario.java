package com.benchify.usuario.domain;

/** Actividad de un usuario que se muestra en su perfil. */
public record EstadisticasUsuario(long bancosPublicados, long resenas, long favoritos, long checkIns) {
}
