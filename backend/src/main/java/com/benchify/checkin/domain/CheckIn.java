package com.benchify.checkin.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.Ubicacion;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** "Estuve aquí": el usuario confirma que está sentado en un banco. */
public record CheckIn(
        UUID id,
        String usuarioId,
        UUID bancoId,
        Ubicacion posicion,
        double distanciaMetros,
        Instant fecha) {

    public CheckIn {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(bancoId, "bancoId");
        Objects.requireNonNull(posicion, "posicion");
        Objects.requireNonNull(fecha, "fecha");
    }

    /**
     * Registra la visita si el usuario está lo bastante cerca del banco y no ha hecho otra
     * hace poco en el mismo banco.
     */
    public static CheckIn registrar(UUID id, String usuarioId, UUID bancoId, Ubicacion banco, Ubicacion posicion,
            Optional<Instant> ultimoCheckIn, PoliticaCheckIn politica, Instant ahora) {
        double distancia = banco.distanciaMetrosA(posicion);
        if (distancia > politica.distanciaMaximaMetros()) {
            throw new BusinessRuleException("CHECKIN_DEMASIADO_LEJOS", "Estás a %d m del banco; acércate a menos de %d m"
                    .formatted(Math.round(distancia), Math.round(politica.distanciaMaximaMetros())));
        }
        ultimoCheckIn
                .filter(ultimo -> ultimo.plus(politica.intervaloMinimo()).isAfter(ahora))
                .ifPresent(ultimo -> {
                    throw new ConflictException("CHECKIN_REPETIDO",
                            "Ya has hecho check-in en este banco hace poco; vuelve más tarde");
                });
        return new CheckIn(id, usuarioId, bancoId, posicion, distancia, ahora);
    }
}
