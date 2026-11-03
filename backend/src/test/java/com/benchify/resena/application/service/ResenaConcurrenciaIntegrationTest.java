package com.benchify.resena.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.resena.application.port.in.ResenasUseCase;
import com.benchify.resena.application.port.in.ResenasUseCase.ResenaCommand;
import com.benchify.resena.application.port.out.ResenaRepository;
import com.benchify.resena.domain.Resena;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.TestData;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Escrituras simultáneas sobre la misma reseña en transacciones reales: a diferencia del resto de
 * tests de integración no hay rollback, así que cada test borra al final lo que ha creado.
 */
class ResenaConcurrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private ResenasUseCase resenasUseCase;
    @Autowired
    private ResenaRepository resenas;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private TestData testData;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService otraPeticion = Executors.newSingleThreadExecutor();
    private TransactionTemplate tx;
    private String autorBanco;
    private String resenador;
    private String reportero;
    private UUID bancoId;
    private UUID resenaId;

    @BeforeEach
    void datos() {
        tx = new TransactionTemplate(transactionManager);
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        autorBanco = "conc-autor-" + sufijo;
        resenador = "conc-resenador-" + sufijo;
        reportero = "conc-reportero-" + sufijo;
        testData.usuario(autorBanco);
        testData.usuario(resenador);
        testData.usuario(reportero);
        bancoId = tx.execute(status -> bancos.save(Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Concurrido", null, new Ubicacion(-45.5, 60.5), null, null, null),
                Actor.admin(autorBanco), AHORA)).id());
        resenaId = resenasUseCase.publicar(Actor.user(resenador), bancoId, new ResenaCommand(4, "Original")).id();
    }

    @AfterEach
    void limpiar() {
        otraPeticion.shutdownNow();
        // Borra en cascada las reseñas y sus reportes
        jdbc.update("DELETE FROM bancos WHERE id = ?", bancoId);
        jdbc.update("DELETE FROM usuarios WHERE id IN (?, ?, ?)", autorBanco, resenador, reportero);
    }

    @Test
    void aReportArrivingDuringAnEditIsNotLost() throws Exception {
        CountDownLatch leida = new CountDownLatch(1);
        CountDownLatch reportada = new CountDownLatch(1);
        Future<Resena> edicion = otraPeticion.submit(() -> tx.execute(status -> {
            // Lee la reseña sin bloquearla, como ResenaService.editar, y guarda cuando ya hay un reporte
            Resena leidaAntes = resenas.findById(resenaId).orElseThrow();
            leida.countDown();
            esperar(reportada);
            return resenas.save(leidaAntes.editar(Actor.user(resenador), 2, "Editada", AHORA.plusSeconds(60)));
        }));
        assertThat(leida.await(10, SECONDS)).isTrue();

        resenasUseCase.reportar(Actor.user(reportero), resenaId);
        reportada.countDown();

        assertThat(edicion.get(10, SECONDS).comentario()).isEqualTo("Editada");
        Resena guardada = resenas.findById(resenaId).orElseThrow();
        assertThat(guardada.puntuacion()).isEqualTo(2);
        assertThat(guardada.numReportes()).isEqualTo(1);
    }

    @Test
    void anEditBasedOnAStaleReadFailsInsteadOfOverwritingAnother() throws Exception {
        CountDownLatch leida = new CountDownLatch(1);
        CountDownLatch editadaPorOtra = new CountDownLatch(1);
        Future<Resena> lenta = otraPeticion.submit(() -> tx.execute(status -> {
            Resena leidaAntes = resenas.findById(resenaId).orElseThrow();
            leida.countDown();
            esperar(editadaPorOtra);
            return resenas.save(leidaAntes.editar(Actor.user(resenador), 1, "Lenta", AHORA.plusSeconds(90)));
        }));
        assertThat(leida.await(10, SECONDS)).isTrue();

        resenasUseCase.editar(Actor.user(resenador), resenaId, new ResenaCommand(5, "Rápida"));
        editadaPorOtra.countDown();

        assertThatThrownBy(() -> lenta.get(10, SECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(e -> assertThat(e.getCause())
                        .isInstanceOfAny(OptimisticLockException.class, OptimisticLockingFailureException.class));
        Resena guardada = resenas.findById(resenaId).orElseThrow();
        assertThat(guardada.comentario()).isEqualTo("Rápida");
        assertThat(guardada.version()).isEqualTo(1);
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, SECONDS)) {
                throw new IllegalStateException("La otra petición no ha terminado a tiempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
