package com.benchify.usuario.application.port.in;

import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;

public interface SincronizarUsuarioUseCase {

    /**
     * Da de alta al usuario si es su primera petición o actualiza email, avatar y último acceso.
     * El rol del token solo cuenta en el alta (o para el primer administrador): después manda el
     * de la base de datos y, si el token trae otro, se corrige el custom claim.
     *
     * @return el usuario con el rol guardado, que es con el que se le autoriza
     */
    Usuario sincronizar(IdentidadUsuario identidad);
}
