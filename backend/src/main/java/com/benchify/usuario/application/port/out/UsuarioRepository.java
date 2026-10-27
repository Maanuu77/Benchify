package com.benchify.usuario.application.port.out;

import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.usuario.domain.Usuario;

import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> findById(String id);

    boolean existsById(String id);

    long count();

    Usuario save(Usuario usuario);

    /** Usuarios ordenados del más reciente al más antiguo. */
    PageResult<Usuario> findAll(PageQuery page);
}
