package com.benchify.favorito.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record FavoritoResponse(
        UUID bancoId,
        String titulo,
        double lat,
        double lng,
        String ciudad,
        double puntuacionMedia,
        int numResenas,
        Instant fecha) {
}
