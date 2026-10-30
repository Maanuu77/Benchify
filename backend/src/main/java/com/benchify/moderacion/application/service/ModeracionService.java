package com.benchify.moderacion.application.service;

import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.ImagenRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.moderacion.application.port.in.ModeracionUseCase;
import com.benchify.moderacion.domain.EstadisticasModeracion;
import com.benchify.resena.application.port.in.ResenasUseCase;
import com.benchify.resena.application.port.out.ResenaRepository;
import com.benchify.resena.domain.Resena;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Orquesta la moderación sobre las demás features: las transiciones de estado son reglas de
 * sus agregados ({@link Banco#aprobar}, {@link Imagen#rechazar}…) y aquí solo se exige el rol.
 */
@Service
@Transactional
public class ModeracionService implements ModeracionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ModeracionService.class);

    private final BancoRepository bancos;
    private final ImagenRepository imagenes;
    private final ImagenesBancoUseCase imagenesBanco;
    private final ResenaRepository resenaRepository;
    private final ResenasUseCase resenas;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public ModeracionService(BancoRepository bancos, ImagenRepository imagenes, ImagenesBancoUseCase imagenesBanco,
            ResenaRepository resenaRepository, ResenasUseCase resenas, UsuarioRepository usuarios, Clock clock) {
        this.bancos = bancos;
        this.imagenes = imagenes;
        this.imagenesBanco = imagenesBanco;
        this.resenaRepository = resenaRepository;
        this.resenas = resenas;
        this.usuarios = usuarios;
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
                usuarios.count());
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
        return bancos.save(banco);
    }

    @Override
    public Banco rechazarBanco(Actor actor, UUID bancoId) {
        exigirAdmin(actor);
        Banco banco = cargarBanco(bancoId);
        banco.rechazar();
        log.info("Bench {} rejected by {}", bancoId, actor.userId());
        return bancos.save(banco);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Resena> resenasReportadas(Actor actor, PageQuery page) {
        exigirAdmin(actor);
        return resenaRepository.findReportadas(page);
    }

    @Override
    public void eliminarResena(Actor actor, UUID resenaId) {
        exigirAdmin(actor);
        resenas.eliminar(actor, resenaId);
    }

    @Override
    public Resena mantenerResena(Actor actor, UUID resenaId) {
        exigirAdmin(actor);
        return resenas.descartarReportes(actor, resenaId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Imagen> imagenesPendientes(Actor actor, PageQuery page) {
        exigirAdmin(actor);
        return imagenes.findByEstado(EstadoImagen.PENDIENTE, page);
    }

    /**
     * Sin transacción propia: aprobar copia los ficheros a la carpeta pública, y la parte de base de
     * datos ya va en transacciones cortas dentro del caso de uso de imágenes.
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Imagen aprobarImagen(Actor actor, UUID imagenId) {
        exigirAdmin(actor);
        return imagenesBanco.aprobar(actor, imagenId);
    }

    @Override
    public void rechazarImagen(Actor actor, UUID imagenId) {
        exigirAdmin(actor);
        imagenesBanco.rechazar(actor, imagenId);
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
