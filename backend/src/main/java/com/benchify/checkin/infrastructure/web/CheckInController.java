package com.benchify.checkin.infrastructure.web;

import com.benchify.checkin.application.port.in.CheckInsUseCase;
import com.benchify.checkin.domain.CheckIn;
import com.benchify.checkin.infrastructure.web.dto.CheckInRequest;
import com.benchify.checkin.infrastructure.web.dto.CheckInResponse;
import com.benchify.checkin.infrastructure.web.dto.TotalCheckInsResponse;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Ubicacion;
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

import java.util.UUID;

@RestController
@Tag(name = "Check-ins", description = "Visitas a bancos validadas por proximidad")
class CheckInController {

    private final CheckInsUseCase checkIns;
    private final CheckInWebMapper mapper;

    CheckInController(CheckInsUseCase checkIns, CheckInWebMapper mapper) {
        this.checkIns = checkIns;
        this.mapper = mapper;
    }

    @PostMapping("/api/v1/bancos/{bancoId}/checkins")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Hacer check-in en un banco",
            description = "La posición debe estar a menos de 150 m del banco; uno cada 12 h por banco.")
    @SecurityRequirement(name = "firebase")
    CheckInResponse registrar(@CurrentActor Actor actor, @PathVariable UUID bancoId,
            @Valid @RequestBody CheckInRequest request) {
        CheckIn checkIn = checkIns.registrar(actor, bancoId, new Ubicacion(request.lat(), request.lng()));
        return mapper.toResponse(checkIn);
    }

    @GetMapping("/api/v1/bancos/{bancoId}/checkins/total")
    @Operation(summary = "Número de check-ins de un banco")
    TotalCheckInsResponse total(@PathVariable UUID bancoId) {
        return new TotalCheckInsResponse(checkIns.totalDeBanco(bancoId));
    }

    @GetMapping("/api/v1/usuarios/me/checkins")
    @Operation(summary = "Mis check-ins, los más recientes primero")
    @SecurityRequirement(name = "firebase")
    PageResponse<CheckInResponse> misCheckIns(@CurrentActor Actor actor, @Valid @ParameterObject PageParams page) {
        return PageResponse.from(checkIns.misCheckIns(actor, page.toQuery()), mapper::toResponse);
    }
}
