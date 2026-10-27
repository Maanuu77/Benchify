package com.benchify.favorito.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.favorito.application.port.in.FavoritoConBanco;
import com.benchify.favorito.application.port.out.FavoritoRepository;
import com.benchify.favorito.domain.Favorito;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final Actor ANA = Actor.user("ana");

    @Mock
    private FavoritoRepository favoritos;
    @Mock
    private BancoRepository bancos;

    private FavoritoService service;
    private Banco publicado;

    @BeforeEach
    void setUp() {
        service = new FavoritoService(favoritos, bancos, Clock.fixed(AHORA, ZoneOffset.UTC));
        publicado = Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Banco", null, new Ubicacion(37, -6), null, null, null), Actor.admin("root"), AHORA);
    }

    @Test
    void marksVisibleBenchAsFavourite() {
        when(bancos.findById(publicado.id())).thenReturn(Optional.of(publicado));

        service.marcar(ANA, publicado.id());

        verify(favoritos).guardar(new Favorito("ana", publicado.id(), AHORA));
    }

    @Test
    void cannotFavouriteAnInvisibleBench() {
        Banco ajenoPendiente = Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Pendiente", null, new Ubicacion(37, -6), null, null, null), Actor.user("luis"), AHORA);
        when(bancos.findById(ajenoPendiente.id())).thenReturn(Optional.of(ajenoPendiente));

        assertThatThrownBy(() -> service.marcar(ANA, ajenoPendiente.id())).isInstanceOf(BancoNotFoundException.class);
        verify(favoritos, never()).guardar(any());
    }

    @Test
    void unmarkAndCheckDelegateToTheRepository() {
        when(favoritos.existe("ana", publicado.id())).thenReturn(true);

        service.desmarcar(ANA, publicado.id());

        verify(favoritos).borrar("ana", publicado.id());
        assertThat(service.esFavorito(ANA, publicado.id())).isTrue();
    }

    @Test
    void listsFavouritesWithTheirBenchesInOrder() {
        Banco otro = Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Otro", null, new Ubicacion(37, -6), null, null, null), Actor.admin("root"), AHORA);
        Favorito f1 = new Favorito("ana", otro.id(), AHORA);
        Favorito f2 = new Favorito("ana", publicado.id(), AHORA.minusSeconds(60));
        when(favoritos.findVisiblesByUsuario("ana", PageQuery.first(10)))
                .thenReturn(new PageResult<>(List.of(f1, f2), 0, 10, 2));
        when(bancos.findAllById(List.of(otro.id(), publicado.id()))).thenReturn(List.of(otro, publicado));

        PageResult<FavoritoConBanco> pagina = service.misFavoritos(ANA, PageQuery.first(10));

        assertThat(pagina.content()).extracting(c -> c.banco().datos().titulo()).containsExactly("Otro", "Banco");
        assertThat(pagina.totalElements()).isEqualTo(2);
    }
}
