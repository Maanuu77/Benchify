package com.benchify.incidencia.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.incidencia.application.port.in.IncidenciasUseCase;
import com.benchify.incidencia.application.port.out.IncidenciaRepository;
import com.benchify.incidencia.domain.EstadoIncidencia;
import com.benchify.incidencia.domain.Incidencia;
import com.benchify.incidencia.domain.TipoIncidencia;
import com.benchify.notificacion.application.port.in.NotificacionesUseCase;
import com.benchify.notificacion.application.port.in.NotificacionesUseCase.NuevaNotificacion;
import com.benchify.notificacion.domain.TipoNotificacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.NotFoundException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class IncidenciaService implements IncidenciasUseCase {

    private final IncidenciaRepository incidencias;
    private final BancoRepository bancos;
    private final NotificacionesUseCase notificaciones;
    private final Clock clock;

    public IncidenciaService(IncidenciaRepository incidencias, BancoRepository bancos,
            NotificacionesUseCase notificaciones, Clock clock) {
        this.incidencias = incidencias;
        this.bancos = bancos;
        this.notificaciones = notificaciones;
        this.clock = clock;
    }

    @Override
    public Incidencia reportar(Actor actor, UUID bancoId, TipoIncidencia tipo, String comentario) {
        actor.exigirEmailVerificado();
        bancoVisible(bancoId, actor);
        if (incidencias.existeAbierta(bancoId, actor.userId())) {
            throw new ConflictException("INCIDENCIA_DUPLICADA",
                    "Ya nos has avisado de un problema con este banco; lo revisaremos pronto");
        }
        Incidencia incidencia = Incidencia.reportar(bancoId, actor, tipo, comentario, clock.instant());
        incidencias.guardar(incidencia);
        return incidencia;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<TipoIncidencia, Long> abiertasPorTipo(UUID bancoId, Actor actor) {
        bancoVisible(bancoId, actor);
        return incidencias.contarAbiertasPorTipo(bancoId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Incidencia> abiertas(Actor actor, PageQuery page) {
        exigirAdmin(actor);
        return incidencias.findAbiertas(page);
    }

    @Override
    public Incidencia cerrar(Actor actor, UUID incidenciaId, EstadoIncidencia resultado) {
        exigirAdmin(actor);
        Incidencia cerrada = incidencias.findById(incidenciaId)
                .orElseThrow(() -> new NotFoundException("INCIDENCIA_NOT_FOUND", "No existe la incidencia " + incidenciaId))
                .cerrar(actor, resultado, clock.instant());
        incidencias.actualizar(cerrada);
        if (!cerrada.usuarioId().equals(actor.userId())) {
            String titulo = bancos.findById(cerrada.bancoId()).map(b -> b.datos().titulo()).orElse("un banco");
            notificaciones.notificar(cerrada.usuarioId(), new NuevaNotificacion(TipoNotificacion.INCIDENCIA_REVISADA,
                    "Hemos revisado tu aviso sobre «" + titulo + "»",
                    resultado == EstadoIncidencia.RESUELTA ? "Gracias, ya está solucionado." : "No hemos encontrado el problema.",
                    "/bancos/" + cerrada.bancoId()));
        }
        return cerrada;
    }

    private Banco bancoVisible(UUID bancoId, Actor actor) {
        return bancos.findById(bancoId)
                .filter(b -> b.esVisiblePara(actor))
                .orElseThrow(() -> new BancoNotFoundException(bancoId));
    }

    private static void exigirAdmin(Actor actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new ForbiddenException("ADMIN_REQUIRED", "Solo un administrador puede revisar incidencias");
        }
    }
}
