package com.benchify.usuario.infrastructure.web.dto;

import java.time.Instant;

/** Perfil visible para cualquiera: sin email ni rol. */
public record PerfilPublicoResponse(
        String id,
        String nombre,
        String avatarUrl,
        String ciudad,
        Instant fechaRegistro) {
}
