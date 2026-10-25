package com.benchify.banco.application.port.in;

import com.benchify.banco.domain.Banco;
import com.benchify.shared.domain.Actor;

import java.util.UUID;

public interface GestionarBancoUseCase {

    Banco proponer(Actor actor, BancoCommand command);

    Banco editar(Actor actor, UUID bancoId, BancoCommand command);

    void eliminar(Actor actor, UUID bancoId);
}
