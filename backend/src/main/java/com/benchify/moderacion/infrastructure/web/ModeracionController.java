package com.benchify.moderacion.infrastructure.web;

import com.benchify.banco.domain.EstadoBanco;
import com.benchify.moderacion.application.port.in.ModeracionUseCase;
import com.benchify.moderacion.domain.EstadisticasModeracion;
import com.benchify.moderacion.infrastructure.web.dto.BancoModeracionResponse;
import com.benchify.moderacion.infrastructure.web.dto.ImagenModeracionResponse;
import com.benchify.moderacion.infrastructure.web.dto.ResenaModeracionResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administración: moderación", description = "Colas de revisión de bancos, reseñas e imágenes")
@SecurityRequirement(name = "firebase")
class ModeracionController {

    private final ModeracionUseCase moderacion;
    private final ModeracionWebMapper mapper;

    ModeracionController(ModeracionUseCase moderacion, ModeracionWebMapper mapper) {
        this.moderacion = moderacion;
        this.mapper = mapper;
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Resumen del panel: pendientes y totales")
    EstadisticasModeracion estadisticas(@CurrentActor Actor actor) {
        return moderacion.estadisticas(actor);
    }

    @GetMapping("/bancos")
    @Operation(summary = "Bancos por estado (por defecto, la cola de pendientes)")
    PageResponse<BancoModeracionResponse> bancos(@CurrentActor Actor actor,
            @RequestParam(defaultValue = "PENDIENTE") EstadoBanco estado, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(moderacion.bancos(actor, estado, page.toQuery()), mapper::toResponse);
    }

    @PostMapping("/bancos/{id}/aprobacion")
    @Operation(summary = "Aprobar y publicar un banco")
    BancoModeracionResponse aprobarBanco(@CurrentActor Actor actor, @PathVariable UUID id) {
        return mapper.toResponse(moderacion.aprobarBanco(actor, id));
    }

    @PostMapping("/bancos/{id}/rechazo")
    @Operation(summary = "Rechazar (ocultar) un banco")
    BancoModeracionResponse rechazarBanco(@CurrentActor Actor actor, @PathVariable UUID id) {
        return mapper.toResponse(moderacion.rechazarBanco(actor, id));
    }

    @GetMapping("/resenas/reportadas")
    @Operation(summary = "Reseñas reportadas, las más reportadas primero")
    PageResponse<ResenaModeracionResponse> resenasReportadas(@CurrentActor Actor actor,
            @Valid @ParameterObject PageParams page) {
        return PageResponse.from(moderacion.resenasReportadas(actor, page.toQuery()), mapper::toResponse);
    }

    @DeleteMapping("/resenas/{id}")
    @Operation(summary = "Borrar una reseña inapropiada")
    ResponseEntity<Void> eliminarResena(@CurrentActor Actor actor, @PathVariable UUID id) {
        moderacion.eliminarResena(actor, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resenas/{id}/revision")
    @Operation(summary = "Mantener una reseña reportada y limpiar sus reportes")
    ResenaModeracionResponse mantenerResena(@CurrentActor Actor actor, @PathVariable UUID id) {
        return mapper.toResponse(moderacion.mantenerResena(actor, id));
    }

    @GetMapping("/imagenes/pendientes")
    @Operation(summary = "Fotos pendientes de revisión, de la más antigua a la más reciente")
    PageResponse<ImagenModeracionResponse> imagenesPendientes(@CurrentActor Actor actor,
            @Valid @ParameterObject PageParams page) {
        return PageResponse.from(moderacion.imagenesPendientes(actor, page.toQuery()), mapper::toResponse);
    }

    @PostMapping("/imagenes/{id}/aprobacion")
    @Operation(summary = "Aprobar una foto")
    ImagenModeracionResponse aprobarImagen(@CurrentActor Actor actor, @PathVariable UUID id) {
        return mapper.toResponse(moderacion.aprobarImagen(actor, id));
    }

    @PostMapping("/imagenes/{id}/rechazo")
    @Operation(summary = "Rechazar una foto y borrar el fichero")
    ResponseEntity<Void> rechazarImagen(@CurrentActor Actor actor, @PathVariable UUID id) {
        moderacion.rechazarImagen(actor, id);
        return ResponseEntity.noContent().build();
    }
}
