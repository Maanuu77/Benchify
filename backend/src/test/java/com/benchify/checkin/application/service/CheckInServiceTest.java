package com.benchify.checkin.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.checkin.application.port.in.CheckInConBanco;
import com.benchify.checkin.application.port.out.CheckInRepository;
import com.benchify.checkin.domain.CheckIn;
import com.benchify.checkin.domain.PoliticaCheckIn;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final Ubicacion POSICION_BANCO = new Ubicacion(37.3825, -5.996);
    private static final Actor ANA = Actor.user("ana");

    @Mock
    private CheckInRepository checkIns;
    @Mock
    private BancoRepository bancos;

    private CheckInService service;
    private Banco banco;

    @BeforeEach
    void setUp() {
        service = new CheckInService(checkIns, bancos, new PoliticaCheckIn(150, Duration.ofHours(12)),
                Clock.fixed(AHORA, ZoneOffset.UTC));
        banco = Banco.proponer(UUID.randomUUID(), new DatosBanco("Torre", null, POSICION_BANCO, null, null, null),
                Actor.admin("root"), AHORA);
        lenient().when(bancos.findById(banco.id())).thenReturn(Optional.of(banco));
        lenient().when(checkIns.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registersCheckInNextToThePublishedBench() {
        when(checkIns.findUltimaFechaForUpdate("ana", banco.id())).thenReturn(Optional.empty());

        CheckIn checkIn = service.registrar(ANA, banco.id(), new Ubicacion(37.3826, -5.996));

        assertThat(checkIn.usuarioId()).isEqualTo("ana");
        assertThat(checkIn.distanciaMetros()).isLessThan(20);
        verify(checkIns).save(checkIn);
    }

    @Test
    void appliesTheCooldownFromTheLastCheckIn() {
        when(checkIns.findUltimaFechaForUpdate("ana", banco.id())).thenReturn(Optional.of(AHORA.minusSeconds(60)));

        assertThatThrownBy(() -> service.registrar(ANA, banco.id(), POSICION_BANCO))
                .isInstanceOf(ConflictException.class);
        verify(checkIns, never()).save(any());
    }

    @Test
    void cannotCheckInAtUnpublishedOrInvisibleBenches() {
        Banco propio = Banco.proponer(UUID.randomUUID(), new DatosBanco("Mío", null, POSICION_BANCO, null, null, null),
                ANA, AHORA);
        when(bancos.findById(propio.id())).thenReturn(Optional.of(propio));

        assertThatThrownBy(() -> service.registrar(ANA, propio.id(), POSICION_BANCO))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_NO_PUBLICADO");
        assertThatThrownBy(() -> service.registrar(Actor.user("luis"), propio.id(), POSICION_BANCO))
                .isInstanceOf(BancoNotFoundException.class);
    }

    @Test
    void listsMyCheckInsWithBenchTitles() {
        CheckIn checkIn = new CheckIn(UUID.randomUUID(), "ana", banco.id(), POSICION_BANCO, 3, AHORA);
        CheckIn huerfano = new CheckIn(UUID.randomUUID(), "ana", UUID.randomUUID(), POSICION_BANCO, 3, AHORA);
        when(checkIns.findByUsuario("ana", PageQuery.first(10)))
                .thenReturn(new PageResult<>(List.of(checkIn, huerfano), 0, 10, 2));
        when(bancos.findAllById(List.of(banco.id(), huerfano.bancoId()))).thenReturn(List.of(banco));

        PageResult<CheckInConBanco> pagina = service.misCheckIns(ANA, PageQuery.first(10));

        assertThat(pagina.content()).extracting(CheckInConBanco::tituloBanco).containsExactly("Torre", null);
    }

    @Test
    void countsCheckInsOfABench() {
        when(checkIns.countByBanco(banco.id())).thenReturn(7L);

        assertThat(service.totalDeBanco(banco.id(), null)).isEqualTo(7);
    }

    @Test
    void theTotalOfAMissingOrInvisibleBenchIs404() {
        UUID inexistente = UUID.randomUUID();
        when(bancos.findById(inexistente)).thenReturn(Optional.empty());
        Banco pendiente = Banco.proponer(UUID.randomUUID(), new DatosBanco("Mío", null, POSICION_BANCO, null, null, null),
                ANA, AHORA);
        when(bancos.findById(pendiente.id())).thenReturn(Optional.of(pendiente));
        when(checkIns.countByBanco(pendiente.id())).thenReturn(0L);

        assertThatThrownBy(() -> service.totalDeBanco(inexistente, ANA)).isInstanceOf(BancoNotFoundException.class);
        assertThatThrownBy(() -> service.totalDeBanco(pendiente.id(), null)).isInstanceOf(BancoNotFoundException.class);
        assertThatThrownBy(() -> service.totalDeBanco(pendiente.id(), Actor.user("luis")))
                .isInstanceOf(BancoNotFoundException.class);
        assertThat(service.totalDeBanco(pendiente.id(), ANA)).isZero();
        verify(checkIns, never()).countByBanco(inexistente);
    }
}
