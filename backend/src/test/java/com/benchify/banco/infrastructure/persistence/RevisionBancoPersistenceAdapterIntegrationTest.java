package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.EtiquetaRepository;
import com.benchify.banco.application.port.out.RevisionBancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.Caracteristicas;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.DireccionPostal;
import com.benchify.banco.domain.MaterialBanco;
import com.benchify.banco.domain.RevisionBanco;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class RevisionBancoPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);
    private static final Actor AUTOR = Actor.user("rev-autor");

    @Autowired
    private RevisionBancoRepository revisiones;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private EtiquetaRepository etiquetas;
    @Autowired
    private TestData testData;

    private Banco banco;

    @BeforeEach
    void datos() {
        testData.usuario("rev-autor");
        testData.usuario("rev-admin");
        banco = bancos.save(Banco.proponer(UUID.randomUUID(), datos("Publicado"), AUTOR, AHORA));
    }

    @Test
    void keepsEveryProposedFieldAndUpdatesTheRevision() {
        DatosBanco propuesta = new DatosBanco("Con todo", "Bajo el naranjo", new Ubicacion(37.3891, -5.9845),
                new DireccionPostal("Calle Feria 1", "Sevilla", "Sevilla", "España"),
                new Caracteristicas(true, true, false, MaterialBanco.MADERA),
                Set.copyOf(etiquetas.findAll().subList(0, 2)));
        RevisionBanco revision = RevisionBanco.proponer(banco.id(), AUTOR, propuesta, AHORA);
        revisiones.guardar(revision);

        assertThat(revisiones.findById(revision.id())).contains(revision);
        assertThat(revisiones.findPendienteByBanco(banco.id())).contains(revision);

        RevisionBanco aprobada = revision.aprobar(Actor.admin("rev-admin"), AHORA.plusSeconds(30));
        revisiones.guardar(aprobada);

        assertThat(revisiones.findById(revision.id())).contains(aprobada);
        assertThat(revisiones.findPendienteByBanco(banco.id())).isEmpty();
    }

    @Test
    void theQueueSkipsDeletedBenchesAndCountsTheRest() {
        long antes = revisiones.countPendientes();
        RevisionBanco revision = RevisionBanco.proponer(banco.id(), AUTOR, datos("Cambio"), AHORA.minusSeconds(86_400 * 365L));
        revisiones.guardar(revision);

        assertThat(revisiones.countPendientes()).isEqualTo(antes + 1);
        assertThat(revisiones.findPendientes(PageQuery.first(1)).content()).containsExactly(revision);

        banco.eliminar(AUTOR);
        bancos.save(banco);

        assertThat(revisiones.countPendientes()).isEqualTo(antes);
        assertThat(revisiones.findPendientes(PageQuery.first(100)).content()).doesNotContain(revision);
    }

    @Test
    void aBenchHasAtMostOnePendingEdit() {
        revisiones.guardar(RevisionBanco.proponer(banco.id(), AUTOR, datos("Una"), AHORA));

        // La segunda llega cuando ya hay una pendiente (p. ej. dos guardados simultáneos del autor)
        assertThatThrownBy(() -> revisiones.guardar(RevisionBanco.proponer(banco.id(), AUTOR, datos("Otra"), AHORA)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static DatosBanco datos(String titulo) {
        return new DatosBanco(titulo, null, new Ubicacion(37.38, -6.0), null, null, null);
    }
}
