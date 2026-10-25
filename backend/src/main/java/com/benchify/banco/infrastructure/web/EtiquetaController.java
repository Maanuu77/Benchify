package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.ConsultarBancosUseCase;
import com.benchify.banco.infrastructure.web.dto.EtiquetaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/etiquetas")
@Tag(name = "Etiquetas", description = "Catálogo de etiquetas de bancos")
class EtiquetaController {

    private final ConsultarBancosUseCase consultarBancos;
    private final BancoWebMapper mapper;

    EtiquetaController(ConsultarBancosUseCase consultarBancos, BancoWebMapper mapper) {
        this.consultarBancos = consultarBancos;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Catálogo completo de etiquetas")
    ResponseEntity<List<EtiquetaResponse>> listar() {
        // El catálogo solo cambia con una migración: se puede cachear en el cliente
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(mapper.toEtiquetaResponses(consultarBancos.etiquetas()));
    }
}
