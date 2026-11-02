package com.benchify.moderacion.application.port.in;

import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.Imagen;
import com.benchify.moderacion.domain.EstadisticasModeracion;
import com.benchify.moderacion.domain.EventoModeracion;
import com.benchify.resena.domain.Resena;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.List;
import java.util.UUID;

/** Todas las operaciones exigen un actor administrador. */
public interface ModeracionUseCase {

    EstadisticasModeracion estadisticas(Actor actor);

    /** Bancos en un estado; los pendientes salen del más antiguo al más reciente (orden de llegada). */
    PageResult<Banco> bancos(Actor actor, EstadoBanco estado, PageQuery page);

    Banco aprobarBanco(Actor actor, UUID bancoId);

    /** @param motivo explicación opcional que se guarda en el historial y se envía al autor */
    Banco rechazarBanco(Actor actor, UUID bancoId, String motivo);

    PageResult<Resena> resenasReportadas(Actor actor, PageQuery page);

    /** La reseña reportada es inapropiada: se borra y se recalcula la valoración del banco. */
    void eliminarResena(Actor actor, UUID resenaId, String motivo);

    /** La reseña reportada es correcta: se mantiene y se limpian sus reportes. */
    Resena mantenerResena(Actor actor, UUID resenaId);

    /** Fotos pendientes, de la más antigua a la más reciente, con las fotos ya subidas que se les parecen. */
    PageResult<ImagenPendiente> imagenesPendientes(Actor actor, PageQuery page);

    Imagen aprobarImagen(Actor actor, UUID imagenId);

    /** Marca la imagen como rechazada y, tras confirmarlo, borra sus ficheros del almacenamiento. */
    void rechazarImagen(Actor actor, UUID imagenId, String motivo);

    /** Historial de decisiones de moderación, de la más reciente a la más antigua. */
    PageResult<EventoModeracion> eventos(Actor actor, PageQuery page);

    /**
     * @param parecidas posibles duplicados, de este u otros bancos y en cualquier estado (una foto
     *                  rechazada que se vuelve a subir también cuenta); los más parecidos primero
     */
    record ImagenPendiente(Imagen imagen, List<Imagen> parecidas) {

        public ImagenPendiente {
            parecidas = List.copyOf(parecidas);
        }
    }
}
