package com.benchify.banco.application.service;

import com.benchify.banco.application.port.out.AlmacenImagenesPort;
import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.ImagenRepository;
import com.benchify.banco.application.port.out.ProcesadorImagenesPort;
import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.banco.domain.ImagenProcesada;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.RecordingTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenBancoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
    private static final ArchivoImagen ARCHIVO = new ArchivoImagen("image/jpeg", JPEG);
    private static final ImagenProcesada PROCESADA = new ImagenProcesada(new byte[]{1, 2}, new byte[]{3});
    private static final Actor ANA = Actor.user("ana");
    private static final Actor LUIS = Actor.user("luis");
    private static final Actor ADMIN = Actor.admin("root");

    @Mock
    private BancoRepository bancos;
    @Mock
    private ImagenRepository imagenes;
    @Mock
    private AlmacenImagenesPort almacen;
    @Mock
    private ProcesadorImagenesPort procesador;

    private final RecordingTransactionManager transacciones = new RecordingTransactionManager();
    private ImagenBancoService service;
    private Banco publicado;
    private Banco pendiente;

    @BeforeEach
    void setUp() {
        service = new ImagenBancoService(bancos, imagenes, almacen, procesador, new BorradoFicherosImagen(almacen),
                transacciones, Clock.fixed(AHORA, ZoneOffset.UTC));
        DatosBanco datos = new DatosBanco("Banco", null, new Ubicacion(37, -6), null, null, null);
        publicado = Banco.proponer(UUID.randomUUID(), datos, ANA, AHORA);
        publicado.aprobar(AHORA);
        pendiente = Banco.proponer(UUID.randomUUID(), datos, ANA, AHORA);
        for (Banco banco : List.of(publicado, pendiente)) {
            lenient().when(bancos.findById(banco.id())).thenReturn(Optional.of(banco));
            lenient().when(bancos.findByIdForUpdate(banco.id())).thenReturn(Optional.of(banco));
        }
        lenient().when(imagenes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(imagenes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(procesador.procesar(ARCHIVO)).thenReturn(PROCESADA);
        lenient().when(almacen.guardar(anyString(), any(), anyString()))
                .thenAnswer(inv -> "https://storage/" + inv.getArgument(0));
    }

    @Test
    void anyUserCanUploadToAPublishedBenchAndItWaitsForModeration() {
        Imagen imagen = service.subir(LUIS, publicado.id(), ARCHIVO);

        assertThat(imagen.ruta()).isEqualTo("bancos/" + publicado.id() + "/" + imagen.id() + ".jpg");
        assertThat(imagen.url()).isEqualTo("https://storage/" + imagen.ruta());
        assertThat(imagen.rutaMiniatura()).endsWith(imagen.id() + "_thumb.jpg");
        assertThat(imagen.urlMiniatura()).isEqualTo("https://storage/" + imagen.rutaMiniatura());
        assertThat(imagen.tamanoBytes()).isEqualTo(2);
        verify(almacen).guardar(imagen.ruta(), PROCESADA.contenido(), "image/jpeg");
        verify(almacen).guardar(imagen.rutaMiniatura(), PROCESADA.miniatura(), "image/jpeg");
        assertThat(imagen.subidaPor()).isEqualTo("luis");
        assertThat(imagen.estado()).isEqualTo(EstadoImagen.PENDIENTE);
        assertThat(imagen.fechaSubida()).isEqualTo(AHORA);
    }

    @Test
    void filesAreUploadedOutsideTheTransactionAndTheRowIsInsertedInsideIt() {
        List<Boolean> subidasEnTransaccion = new ArrayList<>();
        when(almacen.guardar(anyString(), any(), anyString())).thenAnswer(inv -> {
            subidasEnTransaccion.add(TransactionSynchronizationManager.isSynchronizationActive());
            return "https://storage/" + inv.getArgument(0);
        });
        when(imagenes.saveAndFlush(any())).thenAnswer(inv -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return inv.getArgument(0);
        });

        service.subir(LUIS, publicado.id(), ARCHIVO);

        assertThat(subidasEnTransaccion).containsExactly(false, false);
        assertThat(transacciones.commits()).isEqualTo(2);
    }

    @Test
    void onlyTheAuthorCanUploadToAnUnpublishedBench() {
        assertThatThrownBy(() -> service.subir(LUIS, pendiente.id(), ARCHIVO))
                .isInstanceOf(BancoNotFoundException.class);

        assertThat(service.subir(ANA, pendiente.id(), ARCHIVO).bancoId()).isEqualTo(pendiente.id());
    }

    @Test
    void deletedBenchesDoNotAcceptPhotosEvenFromAdmins() {
        publicado.eliminar(ANA);

        assertThatThrownBy(() -> service.subir(ADMIN, publicado.id(), ARCHIVO))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_ELIMINADO");
        verify(almacen, never()).guardar(anyString(), any(), anyString());
    }

    @Test
    void enforcesTheImageLimitBeforeProcessingTheFile() {
        when(imagenes.countActivasByBanco(publicado.id())).thenReturn((long) Imagen.MAX_POR_BANCO);

        assertThatThrownBy(() -> service.subir(ANA, publicado.id(), ARCHIVO))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_MAX_IMAGENES");
        verify(procesador, never()).procesar(any());
        verify(almacen, never()).guardar(anyString(), any(), anyString());
    }

    @Test
    void theLimitIsCheckedAgainWithTheBenchRowLocked() {
        // Otra subida simultánea ocupa el último hueco mientras esta procesaba la foto
        when(imagenes.countActivasByBanco(publicado.id()))
                .thenReturn((long) Imagen.MAX_POR_BANCO - 1, (long) Imagen.MAX_POR_BANCO);

        assertThatThrownBy(() -> service.subir(ANA, publicado.id(), ARCHIVO))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_MAX_IMAGENES");
        verify(bancos).findByIdForUpdate(publicado.id());
        verify(imagenes, never()).saveAndFlush(any());
        verify(almacen).borrar(argThat(ruta -> ruta.startsWith("bancos/" + publicado.id()) && !ruta.contains("_thumb")));
        verify(almacen).borrar(argThat(ruta -> ruta.endsWith("_thumb.jpg")));
        assertThat(transacciones.rollbacks()).isEqualTo(1);
    }

    @Test
    void removesTheStoredFilesIfTheInsertFails() {
        when(imagenes.saveAndFlush(any())).thenThrow(new IllegalStateException("duplicate key"));

        assertThatThrownBy(() -> service.subir(ANA, publicado.id(), ARCHIVO)).isInstanceOf(IllegalStateException.class);
        verify(almacen).borrar(argThat(ruta -> ruta.startsWith("bancos/" + publicado.id()) && !ruta.contains("_thumb")));
        verify(almacen).borrar(argThat(ruta -> ruta.endsWith("_thumb.jpg")));
    }

    @Test
    void removesTheStoredFilesIfTheCommitFails() {
        transacciones.fallarAlConfirmarEscrituras();

        assertThatThrownBy(() -> service.subir(ANA, publicado.id(), ARCHIVO))
                .isInstanceOf(TransactionSystemException.class);
        verify(almacen).borrar(argThat(ruta -> ruta.startsWith("bancos/" + publicado.id()) && !ruta.contains("_thumb")));
        verify(almacen).borrar(argThat(ruta -> ruta.endsWith("_thumb.jpg")));
    }

    @Test
    void removesThePhotoIfTheThumbnailCannotBeUploaded() {
        when(almacen.guardar(startsWith("bancos/"), any(), eq("image/jpeg"))).thenAnswer(inv -> {
            String ruta = inv.getArgument(0);
            if (ruta.endsWith("_thumb.jpg")) {
                throw new IllegalStateException("bucket down");
            }
            return "https://storage/" + ruta;
        });

        assertThatThrownBy(() -> service.subir(ANA, publicado.id(), ARCHIVO)).isInstanceOf(IllegalStateException.class);
        verify(almacen).borrar(argThat(ruta -> ruta.startsWith("bancos/" + publicado.id()) && ruta.endsWith(".jpg")
                && !ruta.contains("_thumb")));
        verify(imagenes, never()).saveAndFlush(any());
    }

    @Test
    void listingFiltersOutImagesTheActorCannotSee() {
        Imagen aprobada = imagen("ana", EstadoImagen.APROBADA);
        Imagen deLuis = imagen("luis", EstadoImagen.PENDIENTE);
        when(imagenes.findActivasByBanco(publicado.id())).thenReturn(List.of(aprobada, deLuis));

        assertThat(service.listar(publicado.id(), null)).containsExactly(aprobada);
        assertThat(service.listar(publicado.id(), LUIS)).containsExactly(aprobada, deLuis);
    }

    @Test
    void uploaderBenchAuthorOrAdminCanDelete() {
        Imagen deLuis = imagen("luis", EstadoImagen.APROBADA);
        when(imagenes.findByIdForUpdate(deLuis.id())).thenReturn(Optional.of(deLuis));

        assertThatThrownBy(() -> service.eliminar(Actor.user("otro"), deLuis.id()))
                .isInstanceOf(ForbiddenException.class);

        service.eliminar(ANA, deLuis.id());
        verify(imagenes).delete(deLuis.id());
        verify(almacen).borrar(deLuis.ruta());
    }

    @Test
    void storageFailuresOnDeleteDoNotBreakTheRequest() {
        Imagen imagen = imagen("ana", EstadoImagen.APROBADA);
        when(imagenes.findByIdForUpdate(imagen.id())).thenReturn(Optional.of(imagen));
        doThrow(new IllegalStateException("bucket down")).when(almacen).borrar(imagen.ruta());

        service.eliminar(ADMIN, imagen.id());

        verify(imagenes).delete(imagen.id());
    }

    @Test
    void onlyAdminsModerate() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.aprobar(ANA, id)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.rechazar(ANA, id)).isInstanceOf(ForbiddenException.class);
        verify(imagenes, never()).findByIdForUpdate(any());
    }

    @Test
    void approvingAnImageMakesItPublic() {
        Imagen pendienteDeLuis = imagen("luis", EstadoImagen.PENDIENTE);
        when(imagenes.findByIdForUpdate(pendienteDeLuis.id())).thenReturn(Optional.of(pendienteDeLuis));

        assertThat(service.aprobar(ADMIN, pendienteDeLuis.id()).estado()).isEqualTo(EstadoImagen.APROBADA);
    }

    @Test
    void rejectingAnImageDeletesItsFilesEvenIfStorageFails() {
        Imagen pendienteDeLuis = imagen("luis", EstadoImagen.PENDIENTE);
        when(imagenes.findByIdForUpdate(pendienteDeLuis.id())).thenReturn(Optional.of(pendienteDeLuis));
        doThrow(new IllegalStateException("bucket down")).when(almacen).borrar(pendienteDeLuis.ruta());

        service.rechazar(ADMIN, pendienteDeLuis.id());

        verify(imagenes).save(pendienteDeLuis.rechazar());
        verify(almacen).borrar(pendienteDeLuis.ruta());
    }

    private Imagen imagen(String autor, EstadoImagen estado) {
        UUID id = UUID.randomUUID();
        return new Imagen(id, publicado.id(), "bancos/" + publicado.id() + "/" + id + ".jpg", "https://storage/" + id,
                "image/jpeg", 6, autor, estado, AHORA);
    }
}
