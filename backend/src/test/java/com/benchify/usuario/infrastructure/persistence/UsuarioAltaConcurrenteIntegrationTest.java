package com.benchify.usuario.infrastructure.persistence;

import com.benchify.shared.domain.Rol;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Primer inicio de sesión con varias peticiones simultáneas (#22). Sin {@code @Transactional}:
 * cada hilo confirma su propia transacción, como en producción. Los UID son únicos por test.
 */
class UsuarioAltaConcurrenteIntegrationTest extends AbstractIntegrationTest {

    private static final int PETICIONES = 8;

    @Autowired
    private SincronizarUsuarioUseCase sincronizarUsuario;
    @Autowired
    private UsuarioRepository repository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;

    private final ExecutorService executor = Executors.newFixedThreadPool(PETICIONES);

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    void simultaneousFirstSyncsOfTheSameUserAllSucceed() throws Exception {
        String uid = "concurrente-" + UUID.randomUUID();
        IdentidadUsuario identidad = new IdentidadUsuario(uid, uid + "@benchify.test", "Ana", null, Rol.USER);
        CyclicBarrier salida = new CyclicBarrier(PETICIONES);

        List<Future<Usuario>> resultados = new ArrayList<>();
        for (int i = 0; i < PETICIONES; i++) {
            resultados.add(executor.submit(() -> {
                salida.await(10, TimeUnit.SECONDS);
                return sincronizarUsuario.sincronizar(identidad);
            }));
        }

        for (Future<Usuario> resultado : resultados) {
            assertThat(resultado.get(30, TimeUnit.SECONDS).id()).isEqualTo(uid);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE id = ?", Long.class, uid)).isOne();
    }

    @Test
    void secondInsertWaitsForTheFirstAndReturnsItsRow() throws Exception {
        String uid = "bloqueo-" + UUID.randomUUID();
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Usuario primera = Usuario.registrar(new IdentidadUsuario(uid, "a@benchify.test", "Ana", null, Rol.ADMIN), ahora);
        Usuario segunda = Usuario.registrar(new IdentidadUsuario(uid, "b@benchify.test", "Bea", null, Rol.USER), ahora);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch insertada = new CountDownLatch(1);
        CountDownLatch confirmar = new CountDownLatch(1);

        Future<Usuario> enCurso = executor.submit(() -> tx.execute(status -> {
            Usuario guardada = repository.registrarSiNoExiste(primera);
            insertada.countDown();
            esperar(confirmar);
            return guardada;
        }));
        assertThat(insertada.await(10, TimeUnit.SECONDS)).isTrue();

        Future<Usuario> esperando = executor.submit((Callable<Usuario>) () ->
                tx.execute(status -> repository.registrarSiNoExiste(segunda)));
        // La clave primaria está reservada por una transacción sin confirmar: el INSERT espera, no falla
        Thread.sleep(300);
        assertThat(esperando.isDone()).isFalse();

        confirmar.countDown();
        assertThat(enCurso.get(30, TimeUnit.SECONDS)).isEqualTo(primera);
        assertThat(esperando.get(30, TimeUnit.SECONDS)).isEqualTo(primera);
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the test");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
