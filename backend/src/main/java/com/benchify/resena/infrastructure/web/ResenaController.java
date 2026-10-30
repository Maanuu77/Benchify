package com.benchify.resena.infrastructure.web;

import com.benchify.resena.application.port.in.ResenasUseCase;
import com.benchify.resena.domain.Resena;
import com.benchify.resena.infrastructure.web.dto.ResenaRequest;
import com.benchify.resena.infrastructure.web.dto.ResenaResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@Tag(name = "Reseñas", description = "Valoraciones de bancos y reportes de contenido inapropiado")
class ResenaController {

    private final ResenasUseCase resenas;
    private final ResenaWebMapper mapper;

    ResenaController(ResenasUseCase resenas, ResenaWebMapper mapper) {
        this.resenas = resenas;
        this.mapper = mapper;
    }

    @GetMapping("/api/v1/bancos/{bancoId}/resenas")
    @Operation(summary = "Reseñas de un banco, las más recientes primero")
    PageResponse<ResenaResponse> deBanco(@PathVariable UUID bancoId, @CurrentActor(required = false) Actor actor,
            @Valid @ParameterObject PageParams page) {
        return PageResponse.from(resenas.deBanco(bancoId, actor, page.toQuery()), mapper::toResponse);
    }

    @GetMapping("/api/v1/bancos/{bancoId}/resenas/mia")
    @Operation(summary = "Mi reseña de un banco",
            description = "404 con código RESENA_NO_ENCONTRADA si todavía no lo has reseñado.")
    @SecurityRequirement(name = "firebase")
    ResenaResponse mia(@CurrentActor Actor actor, @PathVariable UUID bancoId) {
        return mapper.toResponse(resenas.mia(bancoId, actor));
    }

    @GetMapping("/api/v1/resenas/{id}")
    @Operation(summary = "Una reseña", description = "404 si su banco no es visible para quien consulta.")
    ResenaResponse obtener(@PathVariable UUID id, @CurrentActor(required = false) Actor actor) {
        return mapper.toResponse(resenas.obtener(id, actor));
    }

    @PostMapping("/api/v1/bancos/{bancoId}/resenas")
    @Operation(summary = "Reseñar un banco publicado", description = "Una reseña por usuario y banco.")
    @SecurityRequirement(name = "firebase")
    ResponseEntity<ResenaResponse> publicar(@CurrentActor Actor actor, @PathVariable UUID bancoId,
            @Valid @RequestBody ResenaRequest request) {
        Resena resena = resenas.publicar(actor, bancoId, mapper.toCommand(request));
        return ResponseEntity.created(URI.create("/api/v1/resenas/" + resena.id())).body(mapper.toResponse(resena));
    }

    @PutMapping("/api/v1/resenas/{id}")
    @Operation(summary = "Editar una reseña propia")
    @SecurityRequirement(name = "firebase")
    ResenaResponse editar(@CurrentActor Actor actor, @PathVariable UUID id, @Valid @RequestBody ResenaRequest request) {
        return mapper.toResponse(resenas.editar(actor, id, mapper.toCommand(request)));
    }

    @DeleteMapping("/api/v1/resenas/{id}")
    @Operation(summary = "Borrar una reseña propia")
    @SecurityRequirement(name = "firebase")
    ResponseEntity<Void> eliminar(@CurrentActor Actor actor, @PathVariable UUID id) {
        resenas.eliminar(actor, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/resenas/{id}/reportes")
    @Operation(summary = "Reportar una reseña ajena",
            description = "Con tres reportes de usuarios distintos pasa a la cola de moderación.")
    @SecurityRequirement(name = "firebase")
    ResponseEntity<Void> reportar(@CurrentActor Actor actor, @PathVariable UUID id) {
        resenas.reportar(actor, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/usuarios/{usuarioId}/resenas")
    @Operation(summary = "Reseñas escritas por un usuario")
    PageResponse<ResenaResponse> deUsuario(@PathVariable String usuarioId, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(resenas.deUsuario(usuarioId, page.toQuery()), mapper::toResponse);
    }
}
