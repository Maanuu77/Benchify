package com.benchify.favorito.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.favorito.application.port.in.FavoritoConBanco;
import com.benchify.favorito.application.port.in.FavoritosUseCase;
import com.benchify.favorito.application.port.out.FavoritoRepository;
import com.benchify.favorito.domain.Favorito;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class FavoritoService implements FavoritosUseCase {

    private final FavoritoRepository favoritos;
    private final BancoRepository bancos;
    private final Clock clock;

    public FavoritoService(FavoritoRepository favoritos, BancoRepository bancos, Clock clock) {
        this.favoritos = favoritos;
        this.bancos = bancos;
        this.clock = clock;
    }

    @Override
    public void marcar(Actor actor, UUID bancoId) {
        bancos.findById(bancoId)
                .filter(banco -> banco.esVisiblePara(actor))
                .orElseThrow(() -> new BancoNotFoundException(bancoId));
        favoritos.guardar(new Favorito(actor.userId(), bancoId, clock.instant()));
    }

    @Override
    public void desmarcar(Actor actor, UUID bancoId) {
        favoritos.borrar(actor.userId(), bancoId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esFavorito(Actor actor, UUID bancoId) {
        return favoritos.existe(actor.userId(), bancoId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FavoritoConBanco> misFavoritos(Actor actor, PageQuery page) {
        PageResult<Favorito> pagina = favoritos.findVisiblesByUsuario(actor.userId(), page);
        List<UUID> ids = pagina.content().stream().map(Favorito::bancoId).toList();
        Map<UUID, Banco> porId = bancos.findAllById(ids).stream()
                .collect(Collectors.toMap(Banco::id, Function.identity()));
        List<FavoritoConBanco> contenido = pagina.content().stream()
                .filter(favorito -> porId.containsKey(favorito.bancoId()))
                .map(favorito -> new FavoritoConBanco(favorito, porId.get(favorito.bancoId())))
                .toList();
        return new PageResult<>(contenido, pagina.page(), pagina.size(), pagina.totalElements());
    }
}
