package com.benchify.lugar.infrastructure.web;

import com.benchify.lugar.application.port.in.BuscarLugaresUseCase;
import com.benchify.lugar.domain.Encuadre;
import com.benchify.lugar.domain.Lugar;
import com.benchify.lugar.infrastructure.web.dto.LugarResponse;
import com.benchify.shared.domain.Ubicacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Lugares", description = "Búsqueda de direcciones y sitios para mover el mapa")
class LugarController {

    private final BuscarLugaresUseCase lugares;

    LugarController(BuscarLugaresUseCase lugares) {
        this.lugares = lugares;
    }

    @GetMapping("/api/v1/lugares")
    @Operation(summary = "Buscar una dirección o un sitio",
            description = "Datos de OpenStreetMap (Nominatim). 422 BUSQUEDA_INVALIDA si el texto tiene menos de 3 o "
                    + "más de 120 caracteres; 503 GEOCODIFICACION_NO_DISPONIBLE si el servicio no responde.")
    List<LugarResponse> buscar(
            @RequestParam String q,
            @Parameter(description = "Centro del mapa, para priorizar los resultados cercanos")
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String idioma) {
        Ubicacion cerca = lat != null && lng != null ? new Ubicacion(lat, lng) : null;
        return lugares.buscar(q, idioma, cerca).stream().map(LugarController::toResponse).toList();
    }

    private static LugarResponse toResponse(Lugar l) {
        Encuadre e = l.encuadre();
        return new LugarResponse(l.nombre(), l.direccion(), l.ubicacion().lat(), l.ubicacion().lng(),
                e == null ? null : e.sur(), e == null ? null : e.oeste(),
                e == null ? null : e.norte(), e == null ? null : e.este());
    }
}
