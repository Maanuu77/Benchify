package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.EtiquetaRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoCercano;
import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.Caracteristicas;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.DireccionPostal;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.banco.domain.MaterialBanco;
import com.benchify.banco.domain.Valoracion;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Transactional
class BancoPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    // Lugares reales de Sevilla, lejos de cualquier dato de ejemplo (se desplazan al Atlántico)
    private static final double OFFSET_LAT = -10;
    private static final Ubicacion TORRE_DEL_ORO = sevilla(37.38232, -5.99632);
    private static final Ubicacion GIRALDA = sevilla(37.38614, -5.99260);
    private static final Ubicacion PLAZA_ESPANA = sevilla(37.37717, -5.98685);
    private static final Ubicacion LEJOS = sevilla(37.60, -5.70);
    private static final BoundingBox CENTRO = new BoundingBox(27.37, -6.00, 27.39, -5.985);

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);
    private static final Actor AUTOR = Actor.user("autor-it");

    @Autowired
    private BancoRepository bancos;
    @Autowired
    private EtiquetaRepository etiquetas;
    @Autowired
    private TestData testData;

    @BeforeEach
    void usuarios() {
        testData.usuario("autor-it", "Autora IT");
        testData.usuario("otro-it");
    }

    @Test
    void tagCatalogIsSeededByFlywayInAlphabeticalOrder() {
        List<Etiqueta> catalogo = etiquetas.findAll();

        assertThat(catalogo).hasSize(9);
        assertThat(catalogo).extracting(Etiqueta::nombre).isSorted().contains("vistas", "sombra", "romantico");
    }

    @Test
    void savesAndReloadsTheWholeAggregate() {
        Etiqueta vistas = etiquetas.findAllById(List.of(testData.etiquetaId("vistas"))).getFirst();
        Banco banco = Banco.proponer(UUID.randomUUID(), datos("Mirador", TORRE_DEL_ORO, Set.of(vistas), true),
                AUTOR, AHORA);

        Banco guardado = bancos.save(banco);

        assertThat(guardado.autor().nombre()).isEqualTo("Autora IT");
        Banco leido = bancos.findById(banco.id()).orElseThrow();
        assertThat(leido.datos()).isEqualTo(banco.datos());
        assertThat(leido.estado()).isEqualTo(EstadoBanco.PENDIENTE);
        assertThat(leido.fechaSubida()).isEqualTo(AHORA);
        assertThat(leido.valoracion()).isEqualTo(Valoracion.SIN_RESENAS);
    }

    @Test
    void updatesReplaceTagsAndBumpTheVersion() {
        Banco banco = bancos.save(Banco.proponer(UUID.randomUUID(),
                datos("Con etiquetas", TORRE_DEL_ORO, Set.copyOf(etiquetas.findAll().subList(0, 2)), false),
                AUTOR, AHORA));

        banco.editar(datos("Sin etiquetas", TORRE_DEL_ORO, Set.of(), false), AUTOR);
        banco.actualizarValoracion(new Valoracion(4.5, 2));
        Banco actualizado = bancos.save(banco);

        assertThat(actualizado.datos().etiquetas()).isEmpty();
        assertThat(actualizado.datos().titulo()).isEqualTo("Sin etiquetas");
        assertThat(actualizado.valoracion()).isEqualTo(new Valoracion(4.5, 2));
        assertThat(actualizado.version()).isGreaterThan(banco.version());
    }

    @Test
    void mapAreaReturnsOnlyPublishedBenchesInsideTheBoxBestRatedFirst() {
        Banco torre = publicado("Torre", TORRE_DEL_ORO, 3.0, false);
        Banco giralda = publicado("Giralda", GIRALDA, 4.8, true);
        publicado("Fuera", LEJOS, 5.0, true);
        bancos.save(Banco.proponer(UUID.randomUUID(), datos("Pendiente", PLAZA_ESPANA, Set.of(), true), AUTOR, AHORA));

        List<Banco> enArea = bancos.findPublicadosEnArea(CENTRO, FiltroBancos.NINGUNO, 500);

        assertThat(enArea).extracting(Banco::id).containsExactly(giralda.id(), torre.id());
    }

    @Test
    void mapAreaAppliesFiltersAndLimit() {
        publicado("Torre", TORRE_DEL_ORO, 3.0, false);
        Banco giralda = publicado("Giralda", GIRALDA, 4.8, true);

        assertThat(bancos.findPublicadosEnArea(CENTRO, new FiltroBancos(true, null, null, null, null), 500))
                .extracting(Banco::id).containsExactly(giralda.id());
        assertThat(bancos.findPublicadosEnArea(CENTRO, new FiltroBancos(null, null, null, null, 4.0), 500))
                .extracting(Banco::id).containsExactly(giralda.id());
        assertThat(bancos.findPublicadosEnArea(CENTRO, FiltroBancos.NINGUNO, 1)).hasSize(1);
    }

    @Test
    void mapAreaFiltersByTag() {
        Etiqueta sombra = etiquetas.findAllById(List.of(testData.etiquetaId("sombra"))).getFirst();
        Banco conSombra = bancos.save(Banco.proponer(UUID.randomUUID(),
                datos("Sombra", GIRALDA, Set.of(sombra), false), Actor.admin("autor-it"), AHORA));
        publicado("Sin sombra", TORRE_DEL_ORO, 0, false);

        assertThat(bancos.findPublicadosEnArea(CENTRO, new FiltroBancos(null, null, null, sombra.id(), null), 500))
                .extracting(Banco::id).containsExactly(conSombra.id());
    }

    @Test
    void nearbySearchOrdersByGeodesicDistanceWithinRadius() {
        Banco torre = publicado("Torre", TORRE_DEL_ORO, 0, false);
        Banco giralda = publicado("Giralda", GIRALDA, 0, false);
        Banco plaza = publicado("Plaza", PLAZA_ESPANA, 0, false);
        publicado("Fuera", LEJOS, 0, false);

        PageResult<BancoCercano> cercanos = bancos.findPublicadosCercanos(TORRE_DEL_ORO, 1500,
                FiltroBancos.NINGUNO, PageQuery.first(10));

        assertThat(cercanos.totalElements()).isEqualTo(3);
        assertThat(cercanos.content()).extracting(c -> c.banco().id())
                .containsExactly(torre.id(), giralda.id(), plaza.id());
        assertThat(cercanos.content().get(0).distanciaMetros()).isZero();
        assertThat(cercanos.content().get(1).distanciaMetros())
                .isCloseTo(TORRE_DEL_ORO.distanciaMetrosA(GIRALDA), within(5.0));
    }

    @Test
    void nearbySearchPaginatesAndRespectsRadius() {
        publicado("Torre", TORRE_DEL_ORO, 0, false);
        Banco giralda = publicado("Giralda", GIRALDA, 0, false);
        publicado("Plaza", PLAZA_ESPANA, 0, false);

        PageResult<BancoCercano> segunda = bancos.findPublicadosCercanos(TORRE_DEL_ORO, 1500,
                FiltroBancos.NINGUNO, new PageQuery(1, 1));
        assertThat(segunda.content()).extracting(c -> c.banco().id()).containsExactly(giralda.id());
        assertThat(segunda.totalPages()).isEqualTo(3);

        assertThat(bancos.findPublicadosCercanos(TORRE_DEL_ORO, 300, FiltroBancos.NINGUNO, PageQuery.first(10))
                .totalElements()).isEqualTo(1);
    }

    @Test
    void authorListingHidesDeletedAndOptionallyUnpublishedBenches() {
        Banco publicado = publicado("Publicado", TORRE_DEL_ORO, 0, false);
        Banco pendiente = bancos.save(Banco.proponer(UUID.randomUUID(),
                datos("Pendiente", GIRALDA, Set.of(), false), AUTOR, AHORA.plusSeconds(1)));
        Banco eliminado = Banco.proponer(UUID.randomUUID(), datos("Eliminado", GIRALDA, Set.of(), false), AUTOR, AHORA);
        eliminado.eliminar(AUTOR);
        bancos.save(eliminado);

        assertThat(bancos.findByAutor("autor-it", false, PageQuery.first(10)).content())
                .extracting(Banco::id).containsExactly(pendiente.id(), publicado.id());
        assertThat(bancos.findByAutor("autor-it", true, PageQuery.first(10)).content())
                .extracting(Banco::id).containsExactly(publicado.id());
    }

    @Test
    void moderationQueueListsOldestFirstAndCounts() {
        long antes = bancos.countByEstado(EstadoBanco.PENDIENTE);
        Banco viejo = bancos.save(Banco.proponer(UUID.randomUUID(), datos("Viejo", GIRALDA, Set.of(), false),
                AUTOR, Instant.parse("2000-01-01T00:00:00Z")));
        bancos.save(Banco.proponer(UUID.randomUUID(), datos("Nuevo", GIRALDA, Set.of(), false), AUTOR, AHORA));

        assertThat(bancos.countByEstado(EstadoBanco.PENDIENTE)).isEqualTo(antes + 2);
        assertThat(bancos.findByEstado(EstadoBanco.PENDIENTE, PageQuery.first(1)).content())
                .extracting(Banco::id).containsExactly(viejo.id());
        assertThat(bancos.existsPublicado(viejo.id())).isFalse();
    }

    private Banco publicado(String titulo, Ubicacion ubicacion, double media, boolean accesible) {
        Banco banco = Banco.proponer(UUID.randomUUID(), datos(titulo, ubicacion, Set.of(), accesible), AUTOR, AHORA);
        banco.aprobar(AHORA);
        banco.actualizarValoracion(new Valoracion(media, media > 0 ? 1 : 0));
        return bancos.save(banco);
    }

    private static DatosBanco datos(String titulo, Ubicacion ubicacion, Set<Etiqueta> etiquetas, boolean accesible) {
        return new DatosBanco(titulo, "Descripción de " + titulo, ubicacion,
                new DireccionPostal("Calle", "Sevilla", "Sevilla", "España"),
                new Caracteristicas(false, accesible, true, MaterialBanco.PIEDRA), etiquetas);
    }

    private static Ubicacion sevilla(double lat, double lng) {
        return new Ubicacion(lat + OFFSET_LAT, lng);
    }
}
