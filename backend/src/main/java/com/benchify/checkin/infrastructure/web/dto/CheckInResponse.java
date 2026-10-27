package com.benchify.checkin.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(UUID id, UUID bancoId, String tituloBanco, long distanciaMetros, Instant fecha) {
}
