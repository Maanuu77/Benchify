package com.benchify.resena.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.Valoracion;
import com.benchify.resena.application.port.in.ResenasUseCase;
import com.benchify.resena.application.port.out.ResenaRepository;
import com.benchify.resena.domain.Resena;
import com.benchify.resena.domain.ResenaNotFoundException;
import com.benchify.resena.domain.ResumenPuntuacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Las operaciones que cambian la puntuación bloquean primero la fila del banco: así dos
 * reseñas simultáneas no pueden dejar una media calculada con datos viejos.
 */
@Service
@Transactional
public class ResenaService implements ResenasUseCase {

    private final ResenaRepository resenas;
    private final BancoRepository bancos;
    private final Clock clock;

    public ResenaService(ResenaRepository resenas, BancoRepository bancos, Clock clock) {
        this.resenas = resenas;
        this.bancos = bancos;
        this.clock = clock;
    }

    @Override
    public Resena publicar(Actor actor, UUID bancoId, ResenaCommand command) {
        Banco banco = bancos.findByIdForUpdate(bancoId)
                .filter(b -> b.esVisiblePara(actor))
                .orElseThrow(() -> new BancoNotFoundException(bancoId));
        if (!banco.estaPublicado()) {
            throw new BusinessRuleException("BANCO_NO_PUBLICADO", "Solo se pueden reseñar bancos publicados");
        }
        if (banco.autor().id().equals(actor.userId())) {
            throw new BusinessRuleException("RESENA_BANCO_PROPIO", "No puedes reseñar un banco que has subido tú");
        }
        if (resenas.existsByBancoAndAutor(bancoId, actor.userId())) {
            throw new ConflictException("RESENA_DUPLICADA", "Ya has reseñado este banco; edita tu reseña");
        }
        Resena resena = resenas.save(Resena.publicar(UUID.randomUUID(), bancoId, actor, command.puntuacion(),
                command.comentario(), clock.instant()));
        recalcularValoracion(banco);
        return resena;
    }

    @Override
    public Resena editar(Actor actor, UUID resenaId, ResenaCommand command) {
        Resena resena = cargar(resenaId);
        Banco banco = bloquearBanco(resena.bancoId());
        Resena editada = resenas.save(resena.editar(actor, command.puntuacion(), command.comentario(), clock.instant()));
        recalcularValoracion(banco);
        return editada;
    }

    @Override
    public void eliminar(Actor actor, UUID resenaId) {
        Resena resena = cargar(resenaId);
        resena.exigirBorradoPor(actor);
        Banco banco = bloquearBanco(resena.bancoId());
        resenas.delete(resenaId);
        recalcularValoracion(banco);
    }

    @Override
    public Resena reportar(Actor actor, UUID resenaId) {
        Resena resena = resenas.findByIdForUpdate(resenaId).orElseThrow(() -> new ResenaNotFoundException(resenaId));
        resena.exigirReportablePor(actor);
        if (!resenas.registrarReporte(resenaId, actor.userId(), clock.instant())) {
            throw new ConflictException("RESENA_YA_REPORTADA", "Ya has reportado esta reseña");
        }
        return resenas.save(resena.conReporteAdicional());
    }

    @Override
    public Resena descartarReportes(Actor actor, UUID resenaId) {
        if (!actor.isAdmin()) {
            throw new ForbiddenException("ADMIN_REQUIRED", "Solo un administrador puede moderar reseñas");
        }
        Resena resena = cargar(resenaId);
        resenas.borrarReportes(resenaId);
        return resenas.save(resena.sinReportes());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Resena> deBanco(UUID bancoId, Actor actor, PageQuery page) {
        bancos.findById(bancoId)
                .filter(b -> b.esVisiblePara(actor))
                .orElseThrow(() -> new BancoNotFoundException(bancoId));
        return resenas.findByBanco(bancoId, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Resena> deUsuario(String usuarioId, PageQuery page) {
        return resenas.findByAutor(usuarioId, page);
    }

    private Resena cargar(UUID resenaId) {
        return resenas.findById(resenaId).orElseThrow(() -> new ResenaNotFoundException(resenaId));
    }

    private Banco bloquearBanco(UUID bancoId) {
        return bancos.findByIdForUpdate(bancoId).orElseThrow(() -> new BancoNotFoundException(bancoId));
    }

    private void recalcularValoracion(Banco banco) {
        ResumenPuntuacion resumen = resenas.resumen(banco.id());
        banco.actualizarValoracion(new Valoracion(resumen.media(), resumen.total()));
        bancos.save(banco);
    }
}
