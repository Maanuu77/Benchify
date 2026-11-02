package com.benchify.moderacion.infrastructure.web;

import com.benchify.banco.domain.EstadoBanco;
import com.benchify.moderacion.application.port.in.ModeracionUseCase;
import com.benchify.moderacion.domain.EstadisticasModeracion;
import com.benchify.moderacion.domain.EventoModeracion;
import com.benchify.moderacion.infrastructure.web.dto.BancoModeracionResponse;
import com.benchify.moderacion.infrastructure.web.dto.EventoModeracionResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administración: moderación", description = "Colas de revisión de bancos, reseñas e imágenes")
@SecurityRequirement(name = "firebase")
class ModeracionController {

    private static final String MOTIVO = "El parámetro opcional `motivo` (500 caracteres como mucho) se guarda en el "
            + "historial y se envía al autor en su notificación.";

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
    @Operation(summary = "Rechazar (ocultar) un banco", description = MOTIVO)
    BancoModeracionResponse rechazarBanco(@CurrentActor Actor actor, @PathVariable UUID id,
            @RequestParam(required = false) String motivo) {
        return mapper.toResponse(moderacion.rechazarBanco(actor, id, motivo));
    }

    @GetMapping("/resenas/reportadas")
    @Operation(summary = "Reseñas reportadas, las más reportadas primero")
    PageResponse<ResenaModeracionResponse> resenasReportadas(@CurrentActor Actor actor,
            @Valid @ParameterObject PageParams page) {
        return PageResponse.from(moderacion.resenasReportadas(actor, page.toQuery()), mapper::toResponse);
    }

    @DeleteMapping("/resenas/{id}")
    @Operation(summary = "Borrar una reseña inapropiada", description = MOTIVO)
    ResponseEntity<Void> eliminarResena(@CurrentActor Actor actor, @PathVariable UUID id,
            @RequestParam(required = false) String motivo) {
        moderacion.eliminarResena(actor, id, motivo);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resenas/{id}/revision")
    @Operation(summary = "Mantener una reseña reportada y limpiar sus reportes")
    ResenaModeracionResponse mantenerResena(@CurrentActor Actor actor, @PathVariable UUID id) {
        return mapper.toResponse(moderacion.mantenerResena(actor, id));
    }

    @GetMapping("/imagenes/pendientes")
    @Operation(summary = "Fotos pendientes de revisión, de la más antigua a la más reciente",
            description = "Cada una trae hasta 3 fotos ya subidas que se le parecen (posibles duplicados).")
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
    @Operation(summary = "Rechazar una foto y borrar el fichero", description = MOTIVO)
    ResponseEntity<Void> rechazarImagen(@CurrentActor Actor actor, @PathVariable UUID id,
            @RequestParam(required = false) String motivo) {
        moderacion.rechazarImagen(actor, id, motivo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eventos")
    @Operation(summary = "Historial de moderación, de la decisión más reciente a la más antigua")
    PageResponse<EventoModeracionResponse> eventos(@CurrentActor Actor actor, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(moderacion.eventos(actor, page.toQuery()), ModeracionController::toResponse);
    }

    private static EventoModeracionResponse toResponse(EventoModeracion e) {
        return new EventoModeracionResponse(e.id(), e.moderadorId(), e.accion(), e.recursoTipo(), e.recursoId(),
                e.motivo(), e.fecha());
    }
}
