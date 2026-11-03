package com.benchify.banco.application.port.in;

import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.RevisionBanco;
import com.benchify.shared.domain.Actor;

import java.util.Optional;
import java.util.UUID;

public interface GestionarBancoUseCase {

    Banco proponer(Actor actor, BancoCommand command);

    /**
     * Si el autor edita un banco publicado, el banco no cambia: la edición queda pendiente de
     * moderación (sustituyendo a otra suya que aún lo estuviera) y se devuelve en el resultado.
     *
     * @param versionEsperada versión de la que parte el cliente ({@code If-Match}), o {@code null}
     *                        para editar sin comprobarla (gana la última edición)
     * @throws com.benchify.shared.domain.PreconditionFailedException si el banco ya no está en esa versión
     */
    ResultadoEdicion editar(Actor actor, UUID bancoId, BancoCommand command, Long versionEsperada);

    void eliminar(Actor actor, UUID bancoId);

    /** Edición del banco que espera moderación; solo la ven su autor y los administradores. */
    Optional<RevisionBanco> revisionPendiente(Actor actor, UUID bancoId);

    /** @param revisionPendiente la edición que espera moderación, o null si se ha aplicado directamente */
    record ResultadoEdicion(Banco banco, RevisionBanco revisionPendiente) {

        public boolean aplicada() {
            return revisionPendiente == null;
        }
    }
}
