package com.benchify.usuario.infrastructure.web;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.infrastructure.security.CurrentActor;
import com.benchify.shared.infrastructure.web.PageParams;
import com.benchify.shared.infrastructure.web.PageResponse;
import com.benchify.usuario.application.port.in.AdministrarUsuariosUseCase;
import com.benchify.usuario.infrastructure.web.dto.CambiarRolRequest;
import com.benchify.usuario.infrastructure.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@Tag(name = "Administración: usuarios", description = "Listado de usuarios y gestión de roles")
@SecurityRequirement(name = "firebase")
class AdminUsuarioController {

    private final AdministrarUsuariosUseCase administrarUsuarios;
    private final UsuarioWebMapper mapper;

    AdminUsuarioController(AdministrarUsuariosUseCase administrarUsuarios, UsuarioWebMapper mapper) {
        this.administrarUsuarios = administrarUsuarios;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Usuarios registrados, del más reciente al más antiguo")
    PageResponse<UsuarioResponse> listar(@CurrentActor Actor actor, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(administrarUsuarios.listar(actor, page.toQuery()), mapper::toResponse);
    }

    @PutMapping("/{id}/rol")
    @Operation(summary = "Cambiar el rol de un usuario",
            description = "Actualiza el custom claim en Firebase. El usuario verá el cambio al refrescar su ID token.")
    UsuarioResponse cambiarRol(@CurrentActor Actor actor, @PathVariable String id,
            @Valid @RequestBody CambiarRolRequest request) {
        return mapper.toResponse(administrarUsuarios.cambiarRol(actor, id, request.rol()));
    }
}
