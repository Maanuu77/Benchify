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

    /**
     * Alta idempotente: si otra petición simultánea acaba de dar de alta al mismo usuario no
     * falla, y devuelve la fila guardada, sea la recibida o la que ya existía.
     */
    Usuario registrarSiNoExiste(Usuario usuario);

    /** Usuarios ordenados del más reciente al más antiguo. */
    PageResult<Usuario> findAll(PageQuery page);
}
