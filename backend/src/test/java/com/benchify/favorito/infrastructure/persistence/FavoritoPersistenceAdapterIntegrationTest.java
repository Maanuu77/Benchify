package com.benchify.favorito.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.favorito.application.port.out.FavoritoRepository;
import com.benchify.favorito.domain.Favorito;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class FavoritoPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private FavoritoRepository favoritos;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private TestData testData;

    @BeforeEach
    void usuarios() {
        testData.usuario("fav-ana");
        testData.usuario("fav-luis");
    }

    @Test
    void savingTwiceIsIdempotentAndDeleteRemovesIt() {
        UUID banco = banco(Actor.admin("fav-luis")).id();

        favoritos.guardar(new Favorito("fav-ana", banco, AHORA));
        favoritos.guardar(new Favorito("fav-ana", banco, AHORA.plusSeconds(5)));

        assertThat(favoritos.existe("fav-ana", banco)).isTrue();
        assertThat(favoritos.findVisiblesByUsuario("fav-ana", PageQuery.first(10)).totalElements()).isEqualTo(1);

        favoritos.borrar("fav-ana", banco);
        assertThat(favoritos.existe("fav-ana", banco)).isFalse();
    }

    @Test
    void listingHidesBenchesTheUserCanNoLongerSee() {
        Banco publico = banco(Actor.admin("fav-luis"));
        Banco propioPendiente = banco(Actor.user("fav-ana"));
        Banco ajenoOculto = banco(Actor.admin("fav-luis"));
        ajenoOculto.rechazar();
        bancos.save(ajenoOculto);
        Banco eliminado = banco(Actor.user("fav-ana"));
        eliminado.eliminar(Actor.user("fav-ana"));
        bancos.save(eliminado);

        favoritos.guardar(new Favorito("fav-ana", publico.id(), AHORA));
        favoritos.guardar(new Favorito("fav-ana", propioPendiente.id(), AHORA.plusSeconds(1)));
        favoritos.guardar(new Favorito("fav-ana", ajenoOculto.id(), AHORA.plusSeconds(2)));
        favoritos.guardar(new Favorito("fav-ana", eliminado.id(), AHORA.plusSeconds(3)));

        PageResult<Favorito> pagina = favoritos.findVisiblesByUsuario("fav-ana", PageQuery.first(10));

        assertThat(pagina.content()).extracting(Favorito::bancoId)
                .containsExactly(propioPendiente.id(), publico.id());
        assertThat(pagina.totalElements()).isEqualTo(2);
        assertThat(pagina.content().getFirst().fecha()).isEqualTo(AHORA.plusSeconds(1));
    }

    private Banco banco(Actor autor) {
        return bancos.save(Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Fav", null, new Ubicacion(30, 30), null, null, null), autor, AHORA));
    }
}
