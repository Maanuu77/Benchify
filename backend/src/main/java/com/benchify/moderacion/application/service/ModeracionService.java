package com.benchify.moderacion.application.service;

import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.ImagenRepository;
import com.benchify.banco.application.port.out.RevisionBancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.banco.domain.ImagenNotFoundException;
import com.benchify.banco.domain.ImagenProcesada;
import com.benchify.banco.domain.RevisionBanco;
import com.benchify.banco.domain.RevisionNotFoundException;
import com.benchify.moderacion.application.port.in.ModeracionUseCase;
import com.benchify.moderacion.application.port.in.ModeracionUseCase.EdicionPendiente;
import com.benchify.moderacion.application.port.in.ModeracionUseCase.ImagenPendiente;
import com.benchify.moderacion.application.port.out.EventoModeracionRepository;
import com.benchify.moderacion.domain.AccionModeracion;
import com.benchify.moderacion.domain.EstadisticasModeracion;
import com.benchify.moderacion.domain.EventoModeracion;
import com.benchify.notificacion.application.port.in.NotificacionesUseCase;
import com.benchify.notificacion.application.port.in.NotificacionesUseCase.NuevaNotificacion;
import com.benchify.notificacion.domain.TipoNotificacion;
import com.benchify.resena.application.port.in.ResenasUseCase;
import com.benchify.resena.application.port.out.ResenaRepository;
import com.benchify.resena.domain.Resena;
import com.benchify.resena.domain.ResenaNotFoundException;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

/**
 * Orquesta la moderación sobre las demás features: las transiciones de estado son reglas de
 * sus agregados ({@link Banco#aprobar}, {@link Imagen#rechazar}…) y aquí solo se exige el rol.
 * Cada decisión queda en el historial y, si el contenido es de otro usuario, se le avisa.
 */
@Service
@Transactional
public class ModeracionService implements ModeracionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ModeracionService.class);
    /** Posibles duplicados que se enseñan junto a cada foto pendiente. */
    static final int MAX_PARECIDAS = 3;

    private final BancoRepository bancos;
    private final ImagenRepository imagenes;
    private final RevisionBancoRepository revisiones;
    private final ImagenesBancoUseCase imagenesBanco;
    private final ResenaRepository resenaRepository;
    private final ResenasUseCase resenas;
    private final UsuarioRepository usuarios;
    private final EventoModeracionRepository eventos;
    private final NotificacionesUseCase notificaciones;
    private final TransactionTemplate transaccion;
    private final Clock clock;

    public ModeracionService(BancoRepository bancos, ImagenRepository imagenes, RevisionBancoRepository revisiones,
            ImagenesBancoUseCase imagenesBanco,
            ResenaRepository resenaRepository, ResenasUseCase resenas, UsuarioRepository usuarios,
            EventoModeracionRepository eventos, NotificacionesUseCase notificaciones,
            PlatformTransactionManager transacciones, Clock clock) {
        this.bancos = bancos;
        this.imagenes = imagenes;
        this.revisiones = revisiones;
        this.imagenesBanco = imagenesBanco;
        this.resenaRepository = resenaRepository;
        this.resenas = resenas;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.notificaciones = notificaciones;
        this.transaccion = new TransactionTemplate(transacciones);
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public EstadisticasModeracion estadisticas(Actor actor) {
        exigirAdmin(actor);
        return new EstadisticasModeracion(
                bancos.countByEstado(EstadoBanco.PENDIENTE),
                bancos.countByEstado(EstadoBanco.PUBLICADO),
                bancos.countByEstado(EstadoBanco.OCULTO),
                resenaRepository.countReportadas(),
                imagenes.countByEstado(EstadoImagen.PENDIENTE),
                resenaRepository.count(),
                usuarios.count(),
                revisiones.countPendientes());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Banco> bancos(Actor actor, EstadoBanco estado, PageQuery page) {
        exigirAdmin(actor);
        return bancos.findByEstado(estado, page);
    }

    @Override
    public Banco aprobarBanco(Actor actor, UUID bancoId) {
        exigirAdmin(actor);
        Banco banco = cargarBanco(bancoId);
        banco.aprobar(clock.instant());
        log.info("Bench {} approved by {}", bancoId, actor.userId());
        Banco guardado = bancos.save(banco);
        registrar(actor, AccionModeracion.BANCO_APROBADO, bancoId, null);
        avisar(actor, banco.autor().id(), new NuevaNotificacion(TipoNotificacion.BANCO_APROBADO,
                "Tu banco «" + titulo(banco) + "» ya está publicado", null, enlaceBanco(bancoId)));
        return guardado;
    }

    @Override
    public Banco rechazarBanco(Actor actor, UUID bancoId, String motivo) {
        exigirAdmin(actor);
        Banco banco = cargarBanco(bancoId);
        banco.rechazar();
        log.info("Bench {} rejected by {}", bancoId, actor.userId());
        Banco guardado = bancos.save(banco);
        EventoModeracion evento = registrar(actor, AccionModeracion.BANCO_RECHAZADO, bancoId, motivo);
        avisar(actor, banco.autor().id(), new NuevaNotificacion(TipoNotificacion.BANCO_RECHAZADO,
                "Tu banco «" + titulo(banco) + "» no se ha publicado", evento.motivo(), enlaceBanco(bancoId)));
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EdicionPendiente> edicionesPendientes(Actor actor, PageQuery page) {
        exigirAdmin(actor);
        return revisiones.findPendientes(page)
                .map(revision -> new EdicionPendiente(revision, cargarBanco(revision.bancoId())));
    }

    /**
     * La fila del banco se bloquea: así la edición no pisa a otra que el autor o un administrador
     * esté guardando a la vez. Se aplica con los permisos del moderador, que edita directamente.
     */
    @Override
    public Banco aprobarEdicion(Actor actor, UUID revisionId) {
        exigirAdmin(actor);
        RevisionBanco revision = revisiones.findById(revisionId)
                .orElseThrow(() -> new RevisionNotFoundException(revisionId));
        RevisionBanco aprobada = revision.aprobar(actor, clock.instant());
        Banco banco = bancos.findByIdForUpdate(revision.bancoId())
                .orElseThrow(() -> new BancoNotFoundException(revision.bancoId()));
        banco.editar(revision.datos(), actor);
        Banco guardado = bancos.save(banco);
        revisiones.guardar(aprobada);
        log.info("Edit {} of bench {} approved by {}", revisionId, banco.id(), actor.userId());
        registrar(actor, AccionModeracion.EDICION_APROBADA, banco.id(), null);
        avisar(actor, revision.autorId(), new NuevaNotificacion(TipoNotificacion.EDICION_APROBADA,
                "Ya se ven tus cambios en «" + titulo(guardado) + "»", null, enlaceBanco(banco.id())));
        return guardado;
    }

    @Override
    public void rechazarEdicion(Actor actor, UUID revisionId, String motivo) {
        exigirAdmin(actor);
        RevisionBanco revision = revisiones.findById(revisionId)
                .orElseThrow(() -> new RevisionNotFoundException(revisionId));
        revisiones.guardar(revision.rechazar(actor, clock.instant()));
        log.info("Edit {} of bench {} rejected by {}", revisionId, revision.bancoId(), actor.userId());
        EventoModeracion evento = registrar(actor, AccionModeracion.EDICION_RECHAZADA, revision.bancoId(), motivo);
        avisar(actor, revision.autorId(), new NuevaNotificacion(TipoNotificacion.EDICION_RECHAZADA,
                "No hemos aplicado tus cambios en «" + tituloBanco(revision.bancoId()) + "»", evento.motivo(),
                enlaceBanco(revision.bancoId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Resena> resenasReportadas(Actor actor, PageQuery page) {
        exigirAdmin(actor);
        return resenaRepository.findReportadas(page);
    }

    @Override
    public void eliminarResena(Actor actor, UUID resenaId, String motivo) {
        exigirAdmin(actor);
        Resena resena = resenaRepository.findById(resenaId).orElseThrow(() -> new ResenaNotFoundException(resenaId));
        resenas.eliminar(actor, resenaId);
        EventoModeracion evento = registrar(actor, AccionModeracion.RESENA_ELIMINADA, resenaId, motivo);
        avisar(actor, resena.autorId(), new NuevaNotificacion(TipoNotificacion.RESENA_RETIRADA,
                "Hemos retirado tu reseña de «" + tituloBanco(resena.bancoId()) + "»", evento.motivo(),
                enlaceBanco(resena.bancoId())));
    }

    @Override
    public Resena mantenerResena(Actor actor, UUID resenaId) {
        exigirAdmin(actor);
        Resena resena = resenas.descartarReportes(actor, resenaId);
        registrar(actor, AccionModeracion.RESENA_MANTENIDA, resenaId, null);
        return resena;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ImagenPendiente> imagenesPendientes(Actor actor, PageQuery page) {
        exigirAdmin(actor);
        return imagenes.findByEstado(EstadoImagen.PENDIENTE, page).map(imagen -> new ImagenPendiente(imagen,
                imagenes.findParecidas(imagen.id(), ImagenProcesada.DISTANCIA_PARECIDAS, MAX_PARECIDAS)));
    }

    /**
     * Sin transacción propia: aprobar copia los ficheros a la carpeta pública, y la parte de base de
     * datos ya va en transacciones cortas dentro del caso de uso de imágenes. El historial y el aviso
     * se guardan después, en otra transacción corta.
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Imagen aprobarImagen(Actor actor, UUID imagenId) {
        exigirAdmin(actor);
        Imagen imagen = imagenesBanco.aprobar(actor, imagenId);
        transaccion.executeWithoutResult(status -> {
            registrar(actor, AccionModeracion.IMAGEN_APROBADA, imagenId, null);
            avisar(actor, imagen.subidaPor(), new NuevaNotificacion(TipoNotificacion.FOTO_APROBADA,
                    "Tu foto de «" + tituloBanco(imagen.bancoId()) + "» ya es visible", null,
                    enlaceBanco(imagen.bancoId())));
        });
        return imagen;
    }

    @Override
    public void rechazarImagen(Actor actor, UUID imagenId, String motivo) {
        exigirAdmin(actor);
        Imagen imagen = imagenes.findById(imagenId).orElseThrow(() -> new ImagenNotFoundException(imagenId));
        imagenesBanco.rechazar(actor, imagenId);
        EventoModeracion evento = registrar(actor, AccionModeracion.IMAGEN_RECHAZADA, imagenId, motivo);
        avisar(actor, imagen.subidaPor(), new NuevaNotificacion(TipoNotificacion.FOTO_RECHAZADA,
                "Tu foto de «" + tituloBanco(imagen.bancoId()) + "» no se ha publicado", evento.motivo(),
                enlaceBanco(imagen.bancoId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EventoModeracion> eventos(Actor actor, PageQuery page) {
        exigirAdmin(actor);
        return eventos.findRecientes(page);
    }

    private EventoModeracion registrar(Actor actor, AccionModeracion accion, UUID recursoId, String motivo) {
        EventoModeracion evento = EventoModeracion.registrar(actor.userId(), accion, recursoId, motivo, clock.instant());
        eventos.guardar(evento);
        return evento;
    }

    /** Nadie necesita un aviso de lo que acaba de hacer él mismo. */
    private void avisar(Actor actor, String destinatario, NuevaNotificacion notificacion) {
        if (destinatario != null && !destinatario.equals(actor.userId())) {
            notificaciones.notificar(destinatario, notificacion);
        }
    }

    private String tituloBanco(UUID bancoId) {
        return bancos.findById(bancoId).map(ModeracionService::titulo).orElse("un banco");
    }

    private static String titulo(Banco banco) {
        return banco.datos().titulo();
    }

    private static String enlaceBanco(UUID bancoId) {
        return "/bancos/" + bancoId;
    }

    private Banco cargarBanco(UUID bancoId) {
        return bancos.findById(bancoId).orElseThrow(() -> new BancoNotFoundException(bancoId));
    }

    private static void exigirAdmin(Actor actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new ForbiddenException("ADMIN_REQUIRED", "Solo un administrador puede moderar contenido");
        }
    }
}
