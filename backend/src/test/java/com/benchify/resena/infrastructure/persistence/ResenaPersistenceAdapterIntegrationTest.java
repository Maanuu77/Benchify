package com.benchify.resena.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.resena.application.port.out.ResenaRepository;
import com.benchify.resena.domain.Resena;
import com.benchify.resena.domain.ResumenPuntuacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
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
import static org.assertj.core.api.Assertions.within;

@Transactional
class ResenaPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private ResenaRepository resenas;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private TestData testData;

    private UUID bancoId;

    @BeforeEach
    void datos() {
        testData.usuario("res-autor");
        testData.usuario("res-ana", "Ana Reseñas");
        testData.usuario("res-luis");
        bancoId = bancos.save(Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Reseñable", null, new Ubicacion(20, 20), null, null, null),
                Actor.admin("res-autor"), AHORA)).id();
    }

    @Test
    void savesWithAuthorNameAndComputesSummary() {
        Resena guardada = resenas.save(resena("res-ana", 5, AHORA));
        resenas.save(resena("res-luis", 2, AHORA.plusSeconds(1)));

        assertThat(guardada.autorNombre()).isEqualTo("Ana Reseñas");
        assertThat(resenas.existsByBancoAndAutor(bancoId, "res-ana")).isTrue();
        ResumenPuntuacion resumen = resenas.resumen(bancoId);
        assertThat(resumen.total()).isEqualTo(2);
        assertThat(resumen.media()).isCloseTo(3.5, within(0.001));
        assertThat(resenas.findByBanco(bancoId, PageQuery.first(10)).content())
                .extracting(Resena::autorId).containsExactly("res-luis", "res-ana");
    }

    @Test
    void summaryOfABenchWithoutReviewsIsEmpty() {
        assertThat(resenas.resumen(bancoId)).isEqualTo(ResumenPuntuacion.VACIO);
    }

    @Test
    void summarySeesDeletesInTheSameTransaction() {
        Resena resena = resenas.save(resena("res-ana", 4, AHORA));

        resenas.delete(resena.id());

        assertThat(resenas.resumen(bancoId)).isEqualTo(ResumenPuntuacion.VACIO);
        assertThat(resenas.findById(resena.id())).isEmpty();
    }

    @Test
    void reportsAreUniquePerUserAndFeedTheModerationQueue() {
        Resena resena = resenas.save(resena("res-ana", 1, AHORA));
        long antes = resenas.countReportadas();

        assertThat(resenas.registrarReporte(resena.id(), "res-luis", AHORA)).isTrue();
        assertThat(resenas.registrarReporte(resena.id(), "res-luis", AHORA)).isFalse();

        Resena reportada = resenas.findByIdForUpdate(resena.id()).orElseThrow()
                .conReporteAdicional().conReporteAdicional().conReporteAdicional();
        resenas.save(reportada);
        assertThat(resenas.countReportadas()).isEqualTo(antes + 1);
        assertThat(resenas.findReportadas(PageQuery.first(50)).content()).extracting(Resena::id).contains(resena.id());

        resenas.borrarReportes(resena.id());
        assertThat(resenas.registrarReporte(resena.id(), "res-luis", AHORA)).isTrue();
    }

    @Test
    void listsByAuthorNewestFirst() {
        resenas.save(resena("res-ana", 3, AHORA));

        assertThat(resenas.findByAutor("res-ana", PageQuery.first(5)).content())
                .singleElement().extracting(Resena::puntuacion).isEqualTo(3);
    }

    private Resena resena(String autor, int puntuacion, Instant fecha) {
        return Resena.publicar(UUID.randomUUID(), bancoId, Actor.user(autor), puntuacion, "Comentario", fecha);
    }
}
