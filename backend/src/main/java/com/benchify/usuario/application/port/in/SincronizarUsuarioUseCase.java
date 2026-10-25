package com.benchify.usuario.application.port.in;

import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;

public interface SincronizarUsuarioUseCase {

    /** Da de alta al usuario si es su primera petición o actualiza rol/email si han cambiado. */
    Usuario sincronizar(IdentidadUsuario identidad);
}
