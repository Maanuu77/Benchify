package com.benchify.usuario.application.port.in;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.usuario.domain.Usuario;

public interface AdministrarUsuariosUseCase {

    PageResult<Usuario> listar(Actor actor, PageQuery page);

    /**
     * Cambia el rol en la base de datos, que es la fuente de verdad, y se aplica en la siguiente
     * petición del usuario. Tras confirmar, lo publica como custom claim y revoca sus sesiones.
     */
    Usuario cambiarRol(Actor actor, String usuarioId, Rol rol);
}
