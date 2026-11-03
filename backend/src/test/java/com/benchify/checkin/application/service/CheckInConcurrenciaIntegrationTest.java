package com.benchify.checkin.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.checkin.application.port.in.CheckInsUseCase;
import com.benchify.checkin.application.port.out.CheckInRepository;
import com.benchify.checkin.domain.CheckIn;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Check-ins simultáneos del mismo usuario en el mismo banco, en transacciones reales: sin rollback,
 * así que cada test borra al final lo que ha creado.
 */
class CheckInConcurrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);
    private static final Ubicacion POSICION = new Ubicacion(-46.25, 61.75);

    @Autowired
    private CheckInsUseCase checkInsUseCase;
    @Autowired
    private CheckInRepository checkIns;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private TestData testData;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService peticiones = Executors.newFixedThreadPool(2);
    private TransactionTemplate tx;
    private String usuario;
    private UUID bancoId;

    @BeforeEach
    void datos() {
        tx = new TransactionTemplate(transactionManager);
        usuario = "conc-chk-" + UUID.randomUUID().toString().substring(0, 8);
        testData.usuario(usuario);
        bancoId = tx.execute(status -> bancos.save(Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Muy visitado", null, POSICION, null, null, null), Actor.admin(usuario), AHORA)).id());
    }

    @AfterEach
    void limpiar() {
        peticiones.shutdownNow();
        // Borra en cascada los check-ins del banco y del usuario
        jdbc.update("DELETE FROM bancos WHERE id = ?", bancoId);
        jdbc.update("DELETE FROM usuarios WHERE id = ?", usuario);
    }

    @Test
    void aCheckInWaitsForAnotherInProgressAndThenSeesIt() throws Exception {
        CountDownLatch bloqueado = new CountDownLatch(1);
        Future<CheckIn> lenta = peticiones.submit(() -> tx.execute(status -> {
            // Toma el bloqueo como CheckInService.registrar y tarda en guardar
            checkIns.findUltimaFechaForUpdate(usuario, bancoId);
            bloqueado.countDown();
            dormir(500);
            return checkIns.save(new CheckIn(UUID.randomUUID(), usuario, bancoId, POSICION, 1, AHORA));
        }));
        assertThat(bloqueado.await(10, SECONDS)).isTrue();

        assertThatThrownBy(() -> checkInsUseCase.registrar(Actor.user(usuario), bancoId, POSICION))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("CHECKIN_REPETIDO");
        lenta.get(10, SECONDS);
        assertThat(checkInsGuardados()).isEqualTo(1);
    }

    @Test
    void onlyOneOfTwoSimultaneousCheckInsIsStored() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Boolean> peticion = () -> {
            salida.await(10, SECONDS);
            try {
                checkInsUseCase.registrar(Actor.user(usuario), bancoId, POSICION);
                return true;
            } catch (ConflictException e) {
                return false;
            }
        };
        List<Future<Boolean>> resultados = List.of(peticiones.submit(peticion), peticiones.submit(peticion));
        salida.countDown();

        assertThat(List.of(resultados.get(0).get(10, SECONDS), resultados.get(1).get(10, SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        assertThat(checkInsGuardados()).isEqualTo(1);
    }

    private long checkInsGuardados() {
        Long total = jdbc.queryForObject("SELECT count(*) FROM checkins WHERE banco_id = ?", Long.class, bancoId);
        return total == null ? 0 : total;
    }

    private static void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
