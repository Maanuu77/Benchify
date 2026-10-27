package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.ConsultarBancosUseCase;
import com.benchify.banco.application.port.in.GestionarBancoUseCase;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.banco.infrastructure.web.dto.BancoCercanoResponse;
import com.benchify.banco.infrastructure.web.dto.BancoRequest;
import com.benchify.banco.infrastructure.web.dto.BancoResponse;
import com.benchify.banco.infrastructure.web.dto.BancoResumenResponse;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.shared.infrastructure.security.CurrentActor;
import com.benchify.shared.infrastructure.web.PageParams;
import com.benchify.shared.infrastructure.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bancos")
@Tag(name = "Bancos", description = "Mapa, búsqueda por cercanía y gestión de bancos")
class BancoController {

    private final ConsultarBancosUseCase consultarBancos;
    private final GestionarBancoUseCase gestionarBanco;
    private final BancoWebMapper mapper;

    BancoController(ConsultarBancosUseCase consultarBancos, GestionarBancoUseCase gestionarBanco,
            BancoWebMapper mapper) {
        this.consultarBancos = consultarBancos;
        this.gestionarBanco = gestionarBanco;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Bancos publicados dentro del área visible del mapa",
            description = "Devuelve como máximo 500 bancos, los mejor valorados primero.")
    List<BancoResumenResponse> enArea(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double minLat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double minLng,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double maxLat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double maxLng,
            @Valid @ParameterObject FiltroParams filtro) {
        return consultarBancos.enArea(new BoundingBox(minLat, minLng, maxLat, maxLng), filtro.toFiltro())
                .stream().map(mapper::toResumen).toList();
    }

    @GetMapping("/cercanos")
    @Operation(summary = "Bancos publicados cerca de un punto, del más cercano al más lejano")
    PageResponse<BancoCercanoResponse> cercanos(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @Parameter(description = "Radio en metros (1–50.000)")
            @RequestParam(defaultValue = "1000") @Min(1) @Max(ConsultarBancosUseCase.RADIO_MAXIMO_METROS) int radio,
            @Valid @ParameterObject FiltroParams filtro,
            @Valid @ParameterObject PageParams page) {
        return PageResponse.from(
                consultarBancos.cercanos(new Ubicacion(lat, lng), radio, filtro.toFiltro(), page.toQuery()),
                mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha de un banco",
            description = "Los bancos no publicados solo los ven su autor y los administradores.")
    BancoResponse obtener(@PathVariable UUID id, @CurrentActor(required = false) Actor actor) {
        return mapper.toResponse(consultarBancos.obtener(id, actor));
    }

    @PostMapping
    @Operation(summary = "Proponer un banco nuevo", description = "Queda pendiente de moderación.")
    @SecurityRequirement(name = "firebase")
    ResponseEntity<BancoResponse> proponer(@CurrentActor Actor actor, @Valid @RequestBody BancoRequest request) {
        Banco banco = gestionarBanco.proponer(actor, mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(banco.id()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(banco));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un banco propio", description = "Si estaba publicado vuelve a moderación.")
    @SecurityRequirement(name = "firebase")
    BancoResponse editar(@CurrentActor Actor actor, @PathVariable UUID id, @Valid @RequestBody BancoRequest request) {
        return mapper.toResponse(gestionarBanco.editar(actor, id, mapper.toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un banco propio (borrado lógico)")
    @SecurityRequirement(name = "firebase")
    ResponseEntity<Void> eliminar(@CurrentActor Actor actor, @PathVariable UUID id) {
        gestionarBanco.eliminar(actor, id);
        return ResponseEntity.noContent().build();
    }

    /** Filtros comunes del mapa y de la búsqueda por cercanía. */
    record FiltroParams(
            Boolean accesible,
            Boolean conSombra,
            Boolean conRespaldo,
            @Parameter(description = "ID de etiqueta") Long etiqueta,
            @DecimalMin("0") @DecimalMax("5") Double puntuacionMin) {

        FiltroBancos toFiltro() {
            return new FiltroBancos(accesible, conSombra, conRespaldo, etiqueta, puntuacionMin);
        }
    }
}
