package com.benchify.banco.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Edición de un banco publicado que espera moderación. Mientras tanto el banco sigue en el mapa
 * con sus datos anteriores; al aprobarla, {@code datos} sustituye a los del banco.
 *
 * @param autorId      quien propone el cambio (el autor del banco)
 * @param revisadaPor  moderador que la aprobó o rechazó; null mientras está pendiente
 */
public record RevisionBanco(
        UUID id,
        UUID bancoId,
        String autorId,
        DatosBanco datos,
        EstadoRevision estado,
        Instant fecha,
        String revisadaPor,
        Instant fechaRevision) {

    public RevisionBanco {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bancoId, "bancoId");
        Objects.requireNonNull(autorId, "autorId");
        Objects.requireNonNull(datos, "datos");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fecha, "fecha");
    }

    public static RevisionBanco proponer(UUID bancoId, Actor actor, DatosBanco datos, Instant ahora) {
        return new RevisionBanco(UUID.randomUUID(), bancoId, actor.userId(), datos, EstadoRevision.PENDIENTE, ahora,
                null, null);
    }

    /** El autor vuelve a editar antes de que se revise: su nueva versión sustituye a la anterior. */
    public RevisionBanco sustituir(DatosBanco nuevosDatos, Instant ahora) {
        exigirPendiente();
        return new RevisionBanco(id, bancoId, autorId, nuevosDatos, estado, ahora, null, null);
    }

    public RevisionBanco aprobar(Actor moderador, Instant ahora) {
        return cerrar(EstadoRevision.APROBADA, moderador, ahora);
    }

    public RevisionBanco rechazar(Actor moderador, Instant ahora) {
        return cerrar(EstadoRevision.RECHAZADA, moderador, ahora);
    }

    private RevisionBanco cerrar(EstadoRevision resultado, Actor moderador, Instant ahora) {
        exigirPendiente();
        return new RevisionBanco(id, bancoId, autorId, datos, resultado, fecha, moderador.userId(), ahora);
    }

    private void exigirPendiente() {
        if (estado != EstadoRevision.PENDIENTE) {
            throw new BusinessRuleException("REVISION_NO_PENDIENTE", "Esta edición ya se ha revisado");
        }
    }
}
