package com.benchify.favorito.application.port.in;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.UUID;

/** Marcar y desmarcar son idempotentes: repetir la operación no es un error. */
public interface FavoritosUseCase {

    void marcar(Actor actor, UUID bancoId);

    void desmarcar(Actor actor, UUID bancoId);

    boolean esFavorito(Actor actor, UUID bancoId);

    /** Favoritos del actor cuyo banco sigue siendo visible para él, los más recientes primero. */
    PageResult<FavoritoConBanco> misFavoritos(Actor actor, PageQuery page);
}
