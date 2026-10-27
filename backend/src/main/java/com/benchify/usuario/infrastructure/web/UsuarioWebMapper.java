package com.benchify.usuario.infrastructure.web;

import com.benchify.usuario.application.port.in.PerfilUsuarioUseCase.ActualizarPerfilCommand;
import com.benchify.usuario.domain.Usuario;
import com.benchify.usuario.infrastructure.web.dto.ActualizarPerfilRequest;
import com.benchify.usuario.infrastructure.web.dto.PerfilPublicoResponse;
import com.benchify.usuario.infrastructure.web.dto.UsuarioResponse;
import org.mapstruct.Mapper;

@Mapper
interface UsuarioWebMapper {

    UsuarioResponse toResponse(Usuario usuario);

    PerfilPublicoResponse toPerfilPublico(Usuario usuario);

    ActualizarPerfilCommand toCommand(ActualizarPerfilRequest request);
}
