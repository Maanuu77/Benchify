package com.benchify.moderacion.application.port.in;

import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.Imagen;
import com.benchify.moderacion.domain.EstadisticasModeracion;
import com.benchify.resena.domain.Resena;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.UUID;

/** Todas las operaciones exigen un actor administrador. */
public interface ModeracionUseCase {

    EstadisticasModeracion estadisticas(Actor actor);

    /** Bancos en un estado; los pendientes salen del más antiguo al más reciente (orden de llegada). */
    PageResult<Banco> bancos(Actor actor, EstadoBanco estado, PageQuery page);

    Banco aprobarBanco(Actor actor, UUID bancoId);

    Banco rechazarBanco(Actor actor, UUID bancoId);

    PageResult<Resena> resenasReportadas(Actor actor, PageQuery page);

    /** La reseña reportada es inapropiada: se borra y se recalcula la valoración del banco. */
    void eliminarResena(Actor actor, UUID resenaId);

    /** La reseña reportada es correcta: se mantiene y se limpian sus reportes. */
    Resena mantenerResena(Actor actor, UUID resenaId);

    PageResult<Imagen> imagenesPendientes(Actor actor, PageQuery page);

    Imagen aprobarImagen(Actor actor, UUID imagenId);

    /** Marca la imagen como rechazada y, tras confirmarlo, borra sus ficheros del almacenamiento. */
    void rechazarImagen(Actor actor, UUID imagenId);
}
