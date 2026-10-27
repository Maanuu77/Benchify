package com.benchify.favorito.application.port.out;

import com.benchify.favorito.domain.Favorito;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.UUID;

public interface FavoritoRepository {

    /** Inserta el favorito si no existía. */
    void guardar(Favorito favorito);

    void borrar(String usuarioId, UUID bancoId);

    boolean existe(String usuarioId, UUID bancoId);

    /**
     * Favoritos del usuario cuyo banco está publicado o es suyo (y no está eliminado),
     * los más recientes primero.
     */
    PageResult<Favorito> findVisiblesByUsuario(String usuarioId, PageQuery page);
}
