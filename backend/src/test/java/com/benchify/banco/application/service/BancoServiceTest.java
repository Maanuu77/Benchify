package com.benchify.banco.application.service;

import com.benchify.banco.application.port.in.BancoCommand;
import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.EtiquetaRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.banco.domain.MaterialBanco;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.PreconditionFailedException;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BancoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final Etiqueta VISTAS = new Etiqueta(3L, "vistas", "#2ECC71", "landscape");
    private static final Actor ANA = Actor.user("ana");

    @Mock
    private BancoRepository bancos;
    @Mock
    private EtiquetaRepository etiquetas;

    private BancoService service;

    @BeforeEach
    void setUp() {
        service = new BancoService(bancos, etiquetas, Clock.fixed(AHORA, ZoneOffset.UTC));
        lenient().when(bancos.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    static BancoCommand command(Set<Long> etiquetaIds) {
        return new BancoCommand("Banco del Betis", "Vistas a Triana", 37.3826, -6.0034, "Calle Betis", "Sevilla",
                null, null, true, false, true, MaterialBanco.MADERA, etiquetaIds);
    }

    @Test
    void proposesBenchWithResolvedTags() {
        when(etiquetas.findAllById(Set.of(3L))).thenReturn(List.of(VISTAS));

        Banco banco = service.proponer(ANA, command(Set.of(3L)));

        assertThat(banco.estado()).isEqualTo(EstadoBanco.PENDIENTE);
        assertThat(banco.fechaSubida()).isEqualTo(AHORA);
        assertThat(banco.datos().etiquetas()).containsExactly(VISTAS);
        assertThat(banco.datos().direccion().pais()).isEqualTo("España");
        assertThat(banco.datos().caracteristicas().conRespaldo()).isTrue();
        verify(bancos).save(banco);
    }

    @Test
    void rejectsUnknownTags() {
        when(etiquetas.findAllById(Set.of(3L, 99L))).thenReturn(List.of(VISTAS));

        assertThatThrownBy(() -> service.proponer(ANA, command(Set.of(3L, 99L))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("99")
                .extracting("code").isEqualTo("ETIQUETA_DESCONOCIDA");
        verify(bancos, never()).save(any());
    }

    @Test
    void hiddenBenchesLookLikeMissingOnes() {
        Banco pendiente = Banco.proponer(UUID.randomUUID(), service.proponer(ANA, command(null)).datos(), ANA, AHORA);
        when(bancos.findById(pendiente.id())).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> service.obtener(pendiente.id(), null)).isInstanceOf(BancoNotFoundException.class);
        assertThatThrownBy(() -> service.obtener(pendiente.id(), Actor.user("luis")))
                .isInstanceOf(BancoNotFoundException.class);
        assertThat(service.obtener(pendiente.id(), ANA)).isSameAs(pendiente);
    }

    @Test
    void editsAndSoftDeletesOwnBench() {
        Banco banco = service.proponer(ANA, command(null));
        when(bancos.findById(banco.id())).thenReturn(Optional.of(banco));

        Banco editado = service.editar(ANA, banco.id(), new BancoCommand("Otro título", null, 37.38, -6.0,
                null, null, null, null, false, true, false, null, Set.of()), null);
        assertThat(editado.datos().titulo()).isEqualTo("Otro título");

        service.eliminar(ANA, banco.id());
        ArgumentCaptor<Banco> saved = ArgumentCaptor.forClass(Banco.class);
        verify(bancos, atLeastOnce()).save(saved.capture());
        assertThat(saved.getValue().estado()).isEqualTo(EstadoBanco.ELIMINADO);
    }

    @Test
    void editingAMissingBenchIs404() {
        UUID id = UUID.randomUUID();
        when(bancos.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(ANA, id, command(null), null))
                .isInstanceOf(BancoNotFoundException.class);
    }

    @Test
    void anEditWithTheCurrentVersionLocksTheBenchAndSavesIt() {
        Banco banco = conVersion(3);
        when(bancos.findByIdForUpdate(banco.id())).thenReturn(Optional.of(banco));

        assertThat(service.editar(ANA, banco.id(), command(null), 3L).datos().titulo()).isEqualTo("Banco del Betis");
        verify(bancos, never()).findById(any());
        verify(bancos).save(banco);
    }

    @Test
    void anEditBasedOnAnOldVersionFailsWithoutSaving() {
        Banco banco = conVersion(3);
        when(bancos.findByIdForUpdate(banco.id())).thenReturn(Optional.of(banco));

        assertThatThrownBy(() -> service.editar(ANA, banco.id(), command(null), 2L))
                .isInstanceOf(PreconditionFailedException.class)
                .extracting("code").isEqualTo("BANCO_VERSION_OBSOLETA");
        verify(bancos, never()).save(any());
    }

    @Test
    void theVersionOfAnInvisibleBenchIsNeverCompared() {
        Banco banco = conVersion(3);
        when(bancos.findByIdForUpdate(banco.id())).thenReturn(Optional.of(banco));

        assertThatThrownBy(() -> service.editar(Actor.user("luis"), banco.id(), command(null), 2L))
                .isInstanceOf(BancoNotFoundException.class);
    }

    private static Banco conVersion(long version) {
        Banco propuesto = Banco.proponer(UUID.randomUUID(), new DatosBanco("Viejo", null, new Ubicacion(37.38, -6.0),
                null, null, null), ANA, AHORA);
        return new Banco(propuesto.id(), propuesto.datos(), propuesto.estado(), propuesto.autor(),
                propuesto.valoracion(), propuesto.fechaSubida(), propuesto.fechaVerificacion(), version);
    }

    @Test
    void mapQueryIsCappedAtMaxResults() {
        BoundingBox area = new BoundingBox(37.3, -6.1, 37.4, -5.9);
        when(bancos.findPublicadosEnArea(area, FiltroBancos.NINGUNO, 500)).thenReturn(List.of());

        assertThat(service.enArea(area, FiltroBancos.NINGUNO)).isEmpty();
        verify(bancos).findPublicadosEnArea(area, FiltroBancos.NINGUNO, 500);
    }

    @Test
    void nearbySearchValidatesRadius() {
        Ubicacion centro = new Ubicacion(37.38, -6.0);

        assertThatThrownBy(() -> service.cercanos(centro, 0, FiltroBancos.NINGUNO, PageQuery.first(10)))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("RADIO_INVALIDO");
        assertThatThrownBy(() -> service.cercanos(centro, 50_001, FiltroBancos.NINGUNO, PageQuery.first(10)))
                .isInstanceOf(BusinessRuleException.class);

        when(bancos.findPublicadosCercanos(centro, 1000, FiltroBancos.NINGUNO, PageQuery.first(10)))
                .thenReturn(PageResult.empty(PageQuery.first(10)));
        assertThat(service.cercanos(centro, 1000, FiltroBancos.NINGUNO, PageQuery.first(10)).content()).isEmpty();
    }

    @Test
    void ownersSeeTheirPendingBenchesOthersOnlyPublishedOnes() {
        PageQuery page = PageQuery.first(10);
        when(bancos.findByAutor(any(), anyBoolean(), any()))
                .thenReturn(PageResult.empty(page));

        service.deUsuario("ana", ANA, page);
        verify(bancos).findByAutor("ana", false, page);

        service.deUsuario("ana", Actor.user("luis"), page);
        service.deUsuario("ana", null, page);
        verify(bancos, times(2)).findByAutor("ana", true, page);

        service.deUsuario("ana", Actor.admin("root"), page);
        verify(bancos, times(2)).findByAutor("ana", false, page);
    }
}
