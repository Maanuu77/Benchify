package com.benchify.controller;

import com.benchify.dto.*;
import com.benchify.service.BancoService;
import com.benchify.service.ResenaService;
import com.benchify.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bancos")
@RequiredArgsConstructor
public class BancoController {

    private final BancoService bancoService;
    private final ResenaService resenaService;
    private final UsuarioService usuarioService;

    /**
     * Endpoint principal del mapa.
     * Devuelve todos los bancos publicados dentro del bounding box visible.
     * Parámetros: minLng, minLat, maxLng, maxLat (coordenadas WGS84)
     */
    @GetMapping
    public ResponseEntity<List<BancoMapaResponse>> obtenerEnBoundingBox(
            @RequestParam double minLng,
            @RequestParam double minLat,
            @RequestParam double maxLng,
            @RequestParam double maxLat) {
        return ResponseEntity.ok(bancoService.obtenerEnBoundingBox(minLng, minLat, maxLng, maxLat));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BancoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(bancoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<BancoResponse> crear(
            @Valid @RequestBody BancoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bancoService.crear(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BancoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody BancoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bancoService.actualizar(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        bancoService.eliminar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/resenas")
    public ResponseEntity<List<ResenaResponse>> obtenerResenas(@PathVariable UUID id) {
        return ResponseEntity.ok(resenaService.obtenerPorBanco(id));
    }

    @PostMapping("/{id}/resenas")
    public ResponseEntity<ResenaResponse> crearResena(
            @PathVariable UUID id,
            @Valid @RequestBody ResenaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resenaService.crear(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/resenas/{resenaId}/reportar")
    public ResponseEntity<Void> reportarResena(
            @PathVariable UUID id,
            @PathVariable Long resenaId,
            @AuthenticationPrincipal UserDetails userDetails) {
        resenaService.reportar(resenaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/favorito")
    public ResponseEntity<Map<String, Boolean>> toggleFavorito(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean esFavorito = usuarioService.toggleFavorito(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("favorito", esFavorito));
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<Void> checkin(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        usuarioService.registrarCheckin(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/favorito")
    public ResponseEntity<Map<String, Boolean>> esFavorito(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean favorito = userDetails != null
                && usuarioService.esFavorito(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("favorito", favorito));
    }
}
