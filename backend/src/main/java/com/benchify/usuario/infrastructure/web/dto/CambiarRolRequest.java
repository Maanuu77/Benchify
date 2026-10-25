package com.benchify.usuario.infrastructure.web.dto;

import com.benchify.shared.domain.Rol;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(@NotNull(message = "El rol es obligatorio") Rol rol) {
}
