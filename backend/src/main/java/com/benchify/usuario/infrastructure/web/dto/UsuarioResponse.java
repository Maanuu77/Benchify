package com.benchify.usuario.infrastructure.web.dto;

import com.benchify.shared.domain.Rol;

import java.time.Instant;

/** Perfil completo: solo lo ve el propio usuario o un administrador. */
public record UsuarioResponse(
        String id,
        String email,
        String nombre,
        String avatarUrl,
        String ciudad,
        Rol rol,
        Instant fechaRegistro) {
}
