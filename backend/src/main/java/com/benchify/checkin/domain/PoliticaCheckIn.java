package com.benchify.checkin.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Reglas configurables del check-in.
 *
 * @param distanciaMaximaMetros a qué distancia del banco tiene que estar el usuario
 * @param intervaloMinimo       tiempo mínimo entre dos check-ins del mismo usuario en el mismo banco
 */
public record PoliticaCheckIn(double distanciaMaximaMetros, Duration intervaloMinimo) {

    public PoliticaCheckIn {
        if (distanciaMaximaMetros <= 0) {
            throw new IllegalArgumentException("distanciaMaximaMetros must be positive");
        }
        Objects.requireNonNull(intervaloMinimo, "intervaloMinimo");
    }
}
