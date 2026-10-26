package com.benchify.usuario.infrastructure.web;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.infrastructure.security.CurrentActor;
import com.benchify.usuario.application.port.in.PerfilUsuarioUseCase;
import com.benchify.usuario.domain.EstadisticasUsuario;
import com.benchify.usuario.infrastructure.web.dto.ActualizarPerfilRequest;
import com.benchify.usuario.infrastructure.web.dto.PerfilPublicoResponse;
import com.benchify.usuario.infrastructure.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Perfil propio y perfiles públicos")
class UsuarioController {

    private final PerfilUsuarioUseCase perfilUsuario;
    private final UsuarioWebMapper mapper;

    UsuarioController(PerfilUsuarioUseCase perfilUsuario, UsuarioWebMapper mapper) {
        this.perfilUsuario = perfilUsuario;
        this.mapper = mapper;
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado",
            description = "En la primera llamada tras registrarse en Firebase crea el perfil local.")
    @SecurityRequirement(name = "firebase")
    UsuarioResponse me(@CurrentActor Actor actor) {
        return mapper.toResponse(perfilUsuario.obtener(actor.userId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Actualizar nombre y ciudad del perfil propio")
    @SecurityRequirement(name = "firebase")
    UsuarioResponse actualizar(@CurrentActor Actor actor, @Valid @RequestBody ActualizarPerfilRequest request) {
        return mapper.toResponse(perfilUsuario.actualizarPerfil(actor, mapper.toCommand(request)));
    }

    @GetMapping("/{id}/estadisticas")
    @Operation(summary = "Actividad pública de un usuario: bancos, reseñas, favoritos y check-ins")
    EstadisticasUsuario estadisticas(@PathVariable String id) {
        return perfilUsuario.estadisticas(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Perfil público de un usuario")
    PerfilPublicoResponse perfilPublico(@PathVariable String id) {
        return mapper.toPerfilPublico(perfilUsuario.obtener(id));
    }
}
