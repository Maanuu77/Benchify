package com.benchify.banco.application.port.in;

import com.benchify.banco.domain.Banco;
import com.benchify.shared.domain.Actor;

import java.util.UUID;

public interface GestionarBancoUseCase {

    Banco proponer(Actor actor, BancoCommand command);

    /**
     * @param versionEsperada versión de la que parte el cliente ({@code If-Match}), o {@code null}
     *                        para editar sin comprobarla (gana la última edición)
     * @throws com.benchify.shared.domain.PreconditionFailedException si el banco ya no está en esa versión
     */
    Banco editar(Actor actor, UUID bancoId, BancoCommand command, Long versionEsperada);

    void eliminar(Actor actor, UUID bancoId);
}
