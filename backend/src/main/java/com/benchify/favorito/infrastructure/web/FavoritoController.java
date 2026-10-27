package com.benchify.favorito.infrastructure.web;

import com.benchify.favorito.application.port.in.FavoritosUseCase;
import com.benchify.favorito.infrastructure.web.dto.EstadoFavoritoResponse;
import com.benchify.favorito.infrastructure.web.dto.FavoritoResponse;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.infrastructure.security.CurrentActor;
import com.benchify.shared.infrastructure.web.PageParams;
import com.benchify.shared.infrastructure.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Favoritos", description = "Bancos guardados por el usuario")
@SecurityRequirement(name = "firebase")
class FavoritoController {

    private final FavoritosUseCase favoritos;
    private final FavoritoWebMapper mapper;

    FavoritoController(FavoritosUseCase favoritos, FavoritoWebMapper mapper) {
        this.favoritos = favoritos;
        this.mapper = mapper;
    }

    @PutMapping("/api/v1/bancos/{bancoId}/favorito")
    @Operation(summary = "Guardar un banco en favoritos (idempotente)")
    ResponseEntity<Void> marcar(@CurrentActor Actor actor, @PathVariable UUID bancoId) {
        favoritos.marcar(actor, bancoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/bancos/{bancoId}/favorito")
    @Operation(summary = "Quitar un banco de favoritos (idempotente)")
    ResponseEntity<Void> desmarcar(@CurrentActor Actor actor, @PathVariable UUID bancoId) {
        favoritos.desmarcar(actor, bancoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/bancos/{bancoId}/favorito")
    @Operation(summary = "¿Está el banco en mis favoritos?")
    EstadoFavoritoResponse estado(@CurrentActor Actor actor, @PathVariable UUID bancoId) {
        return new EstadoFavoritoResponse(favoritos.esFavorito(actor, bancoId));
    }

    @GetMapping("/api/v1/usuarios/me/favoritos")
    @Operation(summary = "Mis favoritos, los más recientes primero")
    PageResponse<FavoritoResponse> misFavoritos(@CurrentActor Actor actor, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(favoritos.misFavoritos(actor, page.toQuery()), mapper::toResponse);
    }
}
