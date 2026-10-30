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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

    @Mock
    private BancoRepository bancos;
    @Mock
    private ImagenRepository imagenes;
    @Mock
    private AlmacenImagenesPort almacen;
    @Mock
    private ProcesadorImagenesPort procesador;

    private ImagenBancoService service;
    private Banco publicado;
    private Banco pendiente;

    @BeforeEach
    void setUp() {
        service = new ImagenBancoService(bancos, imagenes, almacen, procesador, Clock.fixed(AHORA, ZoneOffset.UTC));
        DatosBanco datos = new DatosBanco("Banco", null, new Ubicacion(37, -6), null, null, null);
        publicado = Banco.proponer(UUID.randomUUID(), datos, ANA, AHORA);
        publicado.aprobar(AHORA);
        pendiente = Banco.proponer(UUID.randomUUID(), datos, ANA, AHORA);
        lenient().when(bancos.findById(publicado.id())).thenReturn(Optional.of(publicado));
        lenient().when(bancos.findById(pendiente.id())).thenReturn(Optional.of(pendiente));
        lenient().when(imagenes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(procesador.procesar(ARCHIVO)).thenReturn(PROCESADA);
    }

    @Test
    void anyUserCanUploadToAPublishedBenchAndItWaitsForModeration() {
        when(almacen.guardar(startsWith("bancos/" + publicado.id() + "/"), any(), eq("image/jpeg")))
                .thenAnswer(inv -> "https://storage/" + inv.getArgument(0));

        Imagen imagen = service.subir(LUIS, publicado.id(), ARCHIVO);

        assertThat(imagen.ruta()).endsWith(imagen.id() + ".jpg");
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
    void onlyTheAuthorCanUploadToAnUnpublishedBench() {
        assertThatThrownBy(() -> service.subir(LUIS, pendiente.id(), ARCHIVO))
                .isInstanceOf(BancoNotFoundException.class);

        when(almacen.guardar(anyString(), any(), anyString())).thenReturn("https://storage/y.jpg");
        assertThat(service.subir(ANA, pendiente.id(), ARCHIVO).bancoId()).isEqualTo(pendiente.id());
    }

    @Test
    void enforcesTheImageLimitPerBench() {
        when(imagenes.countActivasByBanco(publicado.id())).thenReturn((long) Imagen.MAX_POR_BANCO);

        assertThatThrownBy(() -> service.subir(ANA, publicado.id(), ARCHIVO))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_MAX_IMAGENES");
        verify(almacen, never()).guardar(anyString(), any(), anyString());
    }

    @Test
    void removesTheStoredFileIfSavingTheRowFails() {
        when(almacen.guardar(anyString(), any(), anyString())).thenReturn("https://storage/z.jpg");
        when(imagenes.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.subir(ANA, publicado.id(), ARCHIVO)).isInstanceOf(IllegalStateException.class);
        verify(almacen).borrar(argThat(ruta -> ruta.startsWith("bancos/" + publicado.id()) && !ruta.contains("_thumb")));
        verify(almacen).borrar(argThat(ruta -> ruta.endsWith("_thumb.jpg")));
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
        when(imagenes.findById(deLuis.id())).thenReturn(Optional.of(deLuis));

        assertThatThrownBy(() -> service.eliminar(Actor.user("otro"), deLuis.id()))
                .isInstanceOf(ForbiddenException.class);

        service.eliminar(ANA, deLuis.id());
        verify(imagenes).delete(deLuis.id());
        verify(almacen).borrar(deLuis.ruta());
    }

    @Test
    void storageFailuresOnDeleteDoNotBreakTheRequest() {
        Imagen imagen = imagen("ana", EstadoImagen.APROBADA);
        when(imagenes.findById(imagen.id())).thenReturn(Optional.of(imagen));
        doThrow(new IllegalStateException("bucket down")).when(almacen).borrar(imagen.ruta());

        service.eliminar(Actor.admin("root"), imagen.id());

        verify(imagenes).delete(imagen.id());
    }

    private Imagen imagen(String autor, EstadoImagen estado) {
        UUID id = UUID.randomUUID();
        return new Imagen(id, publicado.id(), "bancos/" + publicado.id() + "/" + id + ".jpg", "https://storage/" + id,
                "image/jpeg", 6, autor, estado, AHORA);
    }
}
