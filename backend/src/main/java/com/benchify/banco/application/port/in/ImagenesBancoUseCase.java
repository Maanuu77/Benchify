package com.benchify.banco.application.port.in;

import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.shared.domain.Actor;

import java.util.List;
import java.util.UUID;

public interface ImagenesBancoUseCase {

    Imagen subir(Actor actor, UUID bancoId, ArchivoImagen archivo);

    /** Imágenes que el actor puede ver; {@code actor} es {@code null} si es anónimo. */
    List<Imagen> listar(UUID bancoId, Actor actor);

    /** La puede borrar quien la subió, el autor del banco o un administrador. */
    void eliminar(Actor actor, UUID imagenId);
}
