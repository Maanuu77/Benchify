package com.benchify.incidencia.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.incidencia.application.port.out.IncidenciaRepository;
import com.benchify.incidencia.domain.EstadoIncidencia;
import com.benchify.incidencia.domain.Incidencia;
import com.benchify.incidencia.domain.TipoIncidencia;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class IncidenciaPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private IncidenciaRepository incidencias;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private TestData testData;

    private UUID bancoId;

    @BeforeEach
    void datos() {
        testData.usuario("inc-ana");
        testData.usuario("inc-luis");
        testData.usuario("inc-admin");
        bancoId = bancos.save(Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Con problemas", null, new Ubicacion(20, 20), null, null, null),
                Actor.user("inc-ana"), AHORA)).id();
    }

    @Test
    void storesCountsByTypeAndCloses() {
        Incidencia rota = Incidencia.reportar(bancoId, Actor.user("inc-ana"), TipoIncidencia.ROTO, "Tabla suelta", AHORA);
        Incidencia sucia = Incidencia.reportar(bancoId, Actor.user("inc-luis"), TipoIncidencia.SUCIO, null, AHORA.plusSeconds(5));
        incidencias.guardar(rota);
        incidencias.guardar(sucia);

        assertThat(incidencias.existeAbierta(bancoId, "inc-ana")).isTrue();
        assertThat(incidencias.contarAbiertasPorTipo(bancoId))
                .containsEntry(TipoIncidencia.ROTO, 1L).containsEntry(TipoIncidencia.SUCIO, 1L);
        assertThat(incidencias.findAbiertas(PageQuery.first(100)).content()).contains(rota, sucia);

        Incidencia cerrada = rota.cerrar(Actor.admin("inc-admin"), EstadoIncidencia.RESUELTA, AHORA.plusSeconds(60));
        incidencias.actualizar(cerrada);

        assertThat(incidencias.findById(rota.id())).contains(cerrada);
        assertThat(incidencias.existeAbierta(bancoId, "inc-ana")).isFalse();
        assertThat(incidencias.contarAbiertasPorTipo(bancoId)).doesNotContainKey(TipoIncidencia.ROTO);
    }

    @Test
    void theDatabaseAllowsOnlyOneOpenReportPerUserAndBench() {
        incidencias.guardar(Incidencia.reportar(bancoId, Actor.user("inc-ana"), TipoIncidencia.ROTO, null, AHORA));

        assertThatThrownBy(() -> incidencias.guardar(
                Incidencia.reportar(bancoId, Actor.user("inc-ana"), TipoIncidencia.SUCIO, null, AHORA)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
