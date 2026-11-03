package com.benchify.incidencia.infrastructure.web;

import com.benchify.incidencia.application.port.in.IncidenciasUseCase;
import com.benchify.incidencia.domain.Incidencia;
import com.benchify.incidencia.infrastructure.web.dto.IncidenciaRequest;
import com.benchify.incidencia.infrastructure.web.dto.IncidenciaResponse;
import com.benchify.incidencia.infrastructure.web.dto.ResolucionRequest;
import com.benchify.incidencia.infrastructure.web.dto.ResumenIncidenciasResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Incidencias", description = "Avisos de los usuarios sobre bancos rotos, sucios o desaparecidos")
class IncidenciaController {

    private final IncidenciasUseCase incidencias;

    IncidenciaController(IncidenciasUseCase incidencias) {
        this.incidencias = incidencias;
    }

    @PostMapping("/api/v1/bancos/{bancoId}/incidencias")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Avisar de un problema con un banco",
            description = "Exige email verificado. Un aviso abierto por usuario y banco (409 INCIDENCIA_DUPLICADA).")
    @SecurityRequirement(name = "firebase")
    IncidenciaResponse reportar(@CurrentActor Actor actor, @PathVariable UUID bancoId,
            @Valid @RequestBody IncidenciaRequest request) {
        return toResponse(incidencias.reportar(actor, bancoId, request.tipo(), request.comentario()));
    }

    @GetMapping("/api/v1/bancos/{bancoId}/incidencias/resumen")
    @Operation(summary = "Avisos abiertos de un banco por tipo")
    List<ResumenIncidenciasResponse> resumen(@CurrentActor(required = false) Actor actor, @PathVariable UUID bancoId) {
        return incidencias.abiertasPorTipo(bancoId, actor).entrySet().stream()
                .map(e -> new ResumenIncidenciasResponse(e.getKey(), e.getValue()))
                .toList();
    }

    @GetMapping("/api/v1/admin/incidencias")
    @Operation(summary = "Incidencias abiertas, de la más antigua a la más reciente")
    @SecurityRequirement(name = "firebase")
    PageResponse<IncidenciaResponse> abiertas(@CurrentActor Actor actor, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(incidencias.abiertas(actor, page.toQuery()), IncidenciaController::toResponse);
    }

    @PostMapping("/api/v1/admin/incidencias/{id}/resolucion")
    @Operation(summary = "Cerrar una incidencia como resuelta o descartada; se avisa a quien la reportó")
    @SecurityRequirement(name = "firebase")
    IncidenciaResponse cerrar(@CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody ResolucionRequest request) {
        return toResponse(incidencias.cerrar(actor, id, request.estado()));
    }

    private static IncidenciaResponse toResponse(Incidencia i) {
        return new IncidenciaResponse(i.id(), i.bancoId(), i.usuarioId(), i.tipo(), i.comentario(), i.estado(), i.fecha());
    }
}
