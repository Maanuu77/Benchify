package com.benchify.banco.application.service;

import com.benchify.banco.application.port.in.BancoCommand;
import com.benchify.banco.application.port.in.GestionarBancoUseCase;
import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.application.port.out.ImagenRepository;
import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.moderacion.application.port.in.ModeracionUseCase;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.InMemoryImageStorage;
import com.benchify.support.TestData;
import com.benchify.support.TestImages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sin {@code @Transactional}: cada operación confirma de verdad, como en producción, para probar
 * los bloqueos y los borrados de ficheros tras el commit. Cada test limpia lo que crea.
 */
class ImagenBancoServiceIntegrationTest extends AbstractIntegrationTest {

    private static final ArchivoImagen FOTO = new ArchivoImagen("image/png", TestImages.png(64, 48));

    @Autowired
    private ImagenesBancoUseCase imagenes;
    @Autowired
    private GestionarBancoUseCase gestionBancos;
    @Autowired
    private ModeracionUseCase moderacion;
    @Autowired
    private ImagenRepository imagenRepository;
    @Autowired
    private InMemoryImageStorage storage;
    @Autowired
    private TestData testData;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transaccion;

    private final List<String> usuarios = new ArrayList<>();
    private Actor admin;
    private Actor ana;
    private UUID bancoId;

    @BeforeEach
    void bancoPublicado() {
        admin = Actor.admin(usuario("admin"));
        ana = Actor.user(usuario("ana"));
        bancoId = gestionBancos.proponer(admin, new BancoCommand("Banco concurrido", null, 37.39, -5.99, null, null,
                null, null, false, false, false, null, Set.of())).id();
    }

    @AfterEach
    void limpiar() {
        // Las imágenes, reseñas y demás filas del banco se borran en cascada
        jdbc.update("DELETE FROM bancos WHERE id = ?", bancoId);
        usuarios.forEach(id -> jdbc.update("DELETE FROM usuarios WHERE id = ?", id));
    }

    @Test
    void concurrentUploadsNeverExceedThePhotoLimit() throws Exception {
        for (int i = 0; i < Imagen.MAX_POR_BANCO - 2; i++) {
            imagenes.subir(admin, bancoId, FOTO);
        }
        int intentos = 6;
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Imagen>> subidas = new ArrayList<>();
        try (ExecutorService hilos = Executors.newFixedThreadPool(intentos)) {
            for (int i = 0; i < intentos; i++) {
                Callable<Imagen> subida = () -> {
                    salida.await();
                    return imagenes.subir(ana, bancoId, FOTO);
                };
                subidas.add(hilos.submit(subida));
            }
            salida.countDown();
        }

        int correctas = 0;
        for (Future<Imagen> subida : subidas) {
            try {
                subida.get();
                correctas++;
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(BusinessRuleException.class)
                        .extracting("code").isEqualTo("BANCO_MAX_IMAGENES");
            }
        }
        assertThat(correctas).isEqualTo(2);
        assertThat(imagenRepository.countActivasByBanco(bancoId)).isEqualTo(Imagen.MAX_POR_BANCO);
        // Las subidas rechazadas no dejan ficheros: solo quedan los de las fotos registradas
        assertThat(storage.rutas("bancos/" + bancoId + "/")).hasSize(2 * (Imagen.MAX_POR_BANCO - 2));
        assertThat(storage.rutas("pendientes/" + bancoId + "/")).hasSize(2 * 2);
    }

    @Test
    void aFailedInsertDeletesTheUploadedFiles() {
        // Sin fila en usuarios, el INSERT viola la clave ajena de subida_por
        Actor fantasma = Actor.admin("sin-alta-" + UUID.randomUUID());

        assertThatThrownBy(() -> imagenes.subir(fantasma, bancoId, FOTO))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(storage.rutas("bancos/" + bancoId + "/")).isEmpty();
    }

    @Test
    void deletedFilesSurviveARollbackAndGoAwayAfterTheCommit() {
        Imagen imagen = imagenes.subir(admin, bancoId, FOTO);

        transaccion.executeWithoutResult(status -> {
            imagenes.eliminar(admin, imagen.id());
            assertThat(storage.contiene(imagen.ruta())).isTrue();
            status.setRollbackOnly();
        });
        assertThat(imagenRepository.findById(imagen.id())).isPresent();
        assertThat(storage.contiene(imagen.ruta())).isTrue();

        imagenes.eliminar(admin, imagen.id());
        assertThat(imagenRepository.findById(imagen.id())).isEmpty();
        assertThat(storage.rutas("bancos/" + bancoId + "/")).isEmpty();
    }

    @Test
    void approvingMovesThePendingFilesToThePublicFolder() {
        Imagen pendiente = imagenes.subir(ana, bancoId, FOTO);
        assertThat(storage.rutas("pendientes/" + bancoId + "/")).containsExactlyInAnyOrderElementsOf(pendiente.rutas());
        assertThat(storage.rutas("bancos/" + bancoId + "/")).isEmpty();

        // Por el caso de uso de moderación, sin transacción alrededor, como en producción
        Imagen aprobada = moderacion.aprobarImagen(admin, pendiente.id());

        assertThat(aprobada.rutas()).containsExactly(Imagen.rutaPublica(pendiente.ruta()),
                Imagen.rutaPublica(pendiente.rutaMiniatura()));
        assertThat(imagenRepository.findById(pendiente.id())).contains(aprobada);
        assertThat(storage.rutas("bancos/" + bancoId + "/")).containsExactlyInAnyOrderElementsOf(aprobada.rutas());
        assertThat(storage.rutas("pendientes/" + bancoId + "/")).isEmpty();
        assertThat(imagenes.leerFichero(aprobada.id(), true, null).contenido())
                .isEqualTo(storage.leer(aprobada.rutaMiniatura()).orElseThrow());
    }

    @Test
    void rejectingDeletesThePendingFiles() {
        Imagen pendiente = imagenes.subir(ana, bancoId, FOTO);

        moderacion.rechazarImagen(admin, pendiente.id());

        assertThat(imagenRepository.findById(pendiente.id())).get()
                .extracting(Imagen::estado).isEqualTo(EstadoImagen.RECHAZADA);
        assertThat(storage.rutas("pendientes/" + bancoId + "/")).isEmpty();
    }

    @Test
    void pendingPhotosUploadedBeforeThePrivateFolderAreApprovedInPlace() {
        UUID id = UUID.randomUUID();
        String ruta = "bancos/" + bancoId + "/" + id + ".png";
        String url = storage.guardar(ruta, FOTO.contenido(), "image/png");
        imagenRepository.save(new Imagen(id, bancoId, ruta, url, "image/png", FOTO.tamanoBytes(), ana.userId(),
                EstadoImagen.PENDIENTE, Instant.now()));

        Imagen aprobada = moderacion.aprobarImagen(admin, id);

        assertThat(aprobada.estado()).isEqualTo(EstadoImagen.APROBADA);
        assertThat(aprobada.rutas()).containsExactly(ruta);
        assertThat(storage.rutas("bancos/" + bancoId + "/")).containsExactly(ruta);
    }


    private String usuario(String prefijo) {
        String id = prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
        testData.usuario(id);
        usuarios.add(id);
        return id;
    }
}
