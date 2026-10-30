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

    /**
     * Contenido de la foto (o de su miniatura) si el actor la puede ver. Así se sirven las pendientes,
     * que no son públicas en el almacenamiento, a quien las subió y a los administradores.
     *
     * @param actor quien la pide, o {@code null} si es anónimo
     */
    ArchivoImagen leerFichero(UUID imagenId, boolean miniatura, Actor actor);

    /** La puede borrar quien la subió, el autor del banco o un administrador. */
    void eliminar(Actor actor, UUID imagenId);

    /** Un administrador acepta una imagen pendiente: sus ficheros pasan a la carpeta pública. */
    Imagen aprobar(Actor actor, UUID imagenId);

    /** Un administrador rechaza una imagen pendiente: deja de mostrarse y se borran sus ficheros. */
    void rechazar(Actor actor, UUID imagenId);
}
