package com.benchify.checkin.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.checkin.application.port.out.CheckInRepository;
import com.benchify.checkin.domain.CheckIn;
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
class CheckInPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);
    private static final Ubicacion POSICION = new Ubicacion(40, 40);

    @Autowired
    private CheckInRepository checkIns;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private TestData testData;

    private UUID bancoId;

    @BeforeEach
    void datos() {
        testData.usuario("chk-ana");
        bancoId = bancos.save(Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Visitado", null, POSICION, null, null, null), Actor.admin("chk-ana"), AHORA)).id();
    }

    @Test
    void storesCheckInsAndFindsTheLatestDate() {
        assertThat(checkIns.findUltimaFechaForUpdate("chk-ana", bancoId)).isEmpty();

        checkIns.save(checkIn(AHORA.minusSeconds(3600)));
        CheckIn ultimo = checkIns.save(checkIn(AHORA));

        assertThat(checkIns.findUltimaFechaForUpdate("chk-ana", bancoId)).contains(AHORA);
        assertThat(checkIns.countByBanco(bancoId)).isEqualTo(2);
        PageResult<CheckIn> pagina = checkIns.findByUsuario("chk-ana", PageQuery.first(1));
        assertThat(pagina.content()).containsExactly(ultimo);
        assertThat(pagina.totalElements()).isEqualTo(2);
    }

    private CheckIn checkIn(Instant fecha) {
        return new CheckIn(UUID.randomUUID(), "chk-ana", bancoId, POSICION, 12.5, fecha);
    }
}
