package com.benchify.usuario.application.port.in;

import com.benchify.shared.domain.Actor;
import com.benchify.usuario.domain.EstadisticasUsuario;
import com.benchify.usuario.domain.Usuario;

public interface PerfilUsuarioUseCase {

    Usuario obtener(String usuarioId);

    Usuario actualizarPerfil(Actor actor, ActualizarPerfilCommand command);

    EstadisticasUsuario estadisticas(String usuarioId);

    record ActualizarPerfilCommand(String nombre, String ciudad) {
    }
}
