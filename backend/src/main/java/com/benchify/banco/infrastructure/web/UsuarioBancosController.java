package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.ConsultarBancosUseCase;
import com.benchify.banco.infrastructure.web.dto.BancoResumenResponse;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.infrastructure.security.CurrentActor;
import com.benchify.shared.infrastructure.web.PageParams;
import com.benchify.shared.infrastructure.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Bancos")
class UsuarioBancosController {

    private final ConsultarBancosUseCase consultarBancos;
    private final BancoWebMapper mapper;

    UsuarioBancosController(ConsultarBancosUseCase consultarBancos, BancoWebMapper mapper) {
        this.consultarBancos = consultarBancos;
        this.mapper = mapper;
    }

    @GetMapping("/api/v1/usuarios/{usuarioId}/bancos")
    @Operation(summary = "Bancos subidos por un usuario",
            description = "El propio usuario ve también sus bancos pendientes u ocultos.")
    PageResponse<BancoResumenResponse> deUsuario(@PathVariable String usuarioId,
            @CurrentActor(required = false) Actor actor, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(consultarBancos.deUsuario(usuarioId, actor, page.toQuery()), mapper::toResumen);
    }
}
