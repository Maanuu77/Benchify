package com.benchify.favorito.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Un usuario guarda un banco para encontrarlo después. */
public record Favorito(String usuarioId, UUID bancoId, Instant fecha) {

    public Favorito {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(bancoId, "bancoId");
        Objects.requireNonNull(fecha, "fecha");
    }
}
