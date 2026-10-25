package com.benchify.controller;

import com.benchify.dto.BancoResponse;
import com.benchify.dto.EstadisticasResponse;
import com.benchify.dto.ResenaResponse;
import com.benchify.service.ModeracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('MODERADOR')")
public class AdminController {

    private final ModeracionService moderacionService;

    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EstadisticasResponse> estadisticas() {
        return ResponseEntity.ok(moderacionService.obtenerEstadisticas());
    }

    @GetMapping("/bancos/pendientes")
    public ResponseEntity<List<BancoResponse>> bancosPendientes() {
        return ResponseEntity.ok(moderacionService.obtenerBancosPendientes());
    }

    @PostMapping("/bancos/{id}/aprobar")
    public ResponseEntity<Void> aprobarBanco(@PathVariable UUID id) {
        moderacionService.aprobarBanco(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bancos/{id}/rechazar")
    public ResponseEntity<Void> rechazarBanco(@PathVariable UUID id) {
        moderacionService.rechazarBanco(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resenas/reportadas")
    public ResponseEntity<List<ResenaResponse>> resenasReportadas() {
        return ResponseEntity.ok(moderacionService.obtenerResenasPendientesModeracion());
    }

    @DeleteMapping("/resenas/{id}")
    public ResponseEntity<Void> eliminarResena(@PathVariable Long id) {
        moderacionService.eliminarResena(id);
        return ResponseEntity.noContent().build();
    }
}
