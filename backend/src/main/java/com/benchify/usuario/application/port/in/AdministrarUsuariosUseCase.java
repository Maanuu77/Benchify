package com.benchify.usuario.application.port.in;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.usuario.domain.Usuario;

public interface AdministrarUsuariosUseCase {

    PageResult<Usuario> listar(Actor actor, PageQuery page);

    /** Cambia el rol en el proveedor de identidad (custom claim) y en el perfil local. */
    Usuario cambiarRol(Actor actor, String usuarioId, Rol rol);
}
