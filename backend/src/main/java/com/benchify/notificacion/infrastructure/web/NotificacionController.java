package com.benchify.notificacion.infrastructure.web;

import com.benchify.notificacion.application.port.in.NotificacionesUseCase;
import com.benchify.notificacion.domain.Notificacion;
import com.benchify.notificacion.infrastructure.web.dto.NoLeidasResponse;
import com.benchify.notificacion.infrastructure.web.dto.NotificacionResponse;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.infrastructure.security.CurrentActor;
import com.benchify.shared.infrastructure.web.PageParams;
import com.benchify.shared.infrastructure.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Notificaciones", description = "Avisos al usuario, como el resultado de la moderación de su contenido")
@SecurityRequirement(name = "firebase")
class NotificacionController {

    private final NotificacionesUseCase notificaciones;

    NotificacionController(NotificacionesUseCase notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping("/api/v1/usuarios/me/notificaciones")
    @Operation(summary = "Mis notificaciones, de la más reciente a la más antigua")
    PageResponse<NotificacionResponse> mias(@CurrentActor Actor actor, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(notificaciones.mias(actor, page.toQuery()), NotificacionController::toResponse);
    }

    @GetMapping("/api/v1/usuarios/me/notificaciones/no-leidas")
    @Operation(summary = "Cuántas notificaciones no he leído (para el contador de la cabecera)")
    NoLeidasResponse noLeidas(@CurrentActor Actor actor) {
        return new NoLeidasResponse(notificaciones.noLeidas(actor));
    }

    @PostMapping("/api/v1/notificaciones/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar una notificación como leída")
    void marcarLeida(@CurrentActor Actor actor, @PathVariable UUID id) {
        notificaciones.marcarLeida(actor, id);
    }

    @PostMapping("/api/v1/usuarios/me/notificaciones/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar todas mis notificaciones como leídas")
    void marcarTodasLeidas(@CurrentActor Actor actor) {
        notificaciones.marcarTodasLeidas(actor);
    }

    private static NotificacionResponse toResponse(Notificacion n) {
        return new NotificacionResponse(n.id(), n.tipo(), n.titulo(), n.mensaje(), n.enlace(), n.leida(), n.fecha());
    }
}
