package com.benchify.resena.application.port.in;

import com.benchify.resena.domain.Resena;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.UUID;

public interface ResenasUseCase {

    Resena publicar(Actor actor, UUID bancoId, ResenaCommand command);

    Resena editar(Actor actor, UUID resenaId, ResenaCommand command);

    /** Borra la reseña (autor o administrador) y recalcula la valoración del banco. */
    void eliminar(Actor actor, UUID resenaId);

    /** Un usuario reporta una reseña ajena; cada usuario solo cuenta una vez. */
    Resena reportar(Actor actor, UUID resenaId);

    /** Un administrador revisa una reseña reportada y decide mantenerla. */
    Resena descartarReportes(Actor actor, UUID resenaId);

    /**
     * Una reseña es visible si lo es su banco.
     *
     * @param actor quien consulta, o {@code null}
     */
    Resena obtener(UUID resenaId, Actor actor);

    /**
     * Reseña del actor en el banco, esté en la página que esté del listado.
     *
     * @throws com.benchify.resena.domain.ResenaNotFoundException si todavía no lo ha reseñado
     */
    Resena mia(UUID bancoId, Actor actor);

    /** @param actor quien consulta, o {@code null}; el banco debe ser visible para él */
    PageResult<Resena> deBanco(UUID bancoId, Actor actor, PageQuery page);

    /** Reseñas de un usuario en bancos publicados, las más recientes primero. */
    PageResult<Resena> deUsuario(String usuarioId, PageQuery page);

    record ResenaCommand(int puntuacion, String comentario) {
    }
}
