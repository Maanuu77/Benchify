package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.ImagenRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
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
class ImagenPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private ImagenRepository imagenes;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private TestData testData;

    private UUID bancoId;

    @BeforeEach
    void banco() {
        testData.usuario("img-it");
        bancoId = bancos.save(Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Con fotos", null, new Ubicacion(10, 10), null, null, null),
                Actor.user("img-it"), AHORA)).id();
    }

    @Test
    void storesAndListsActiveImagesOldestFirst() {
        Imagen primera = imagenes.save(imagen(EstadoImagen.APROBADA, AHORA));
        Imagen segunda = imagenes.save(imagen(EstadoImagen.PENDIENTE, AHORA.plusSeconds(1)));
        imagenes.save(imagen(EstadoImagen.RECHAZADA, AHORA.plusSeconds(2)));

        assertThat(imagenes.findActivasByBanco(bancoId)).containsExactly(primera, segunda);
        assertThat(imagenes.countActivasByBanco(bancoId)).isEqualTo(2);
        assertThat(imagenes.findById(primera.id())).contains(primera);
    }

    @Test
    void storesTheThumbnailAndKeepsLegacyRowsWithoutOne() {
        UUID id = UUID.randomUUID();
        String ruta = Imagen.rutaPara(bancoId, id, EstadoImagen.APROBADA);
        Imagen conMiniatura = imagenes.save(new Imagen(id, bancoId, ruta, "https://storage.test/" + ruta,
                Imagen.rutaMiniatura(ruta), "https://storage.test/thumb/" + id, "image/jpeg", 1234, "img-it",
                EstadoImagen.APROBADA, AHORA));
        Imagen antigua = imagenes.save(imagen(EstadoImagen.APROBADA, AHORA.plusSeconds(1)));

        assertThat(imagenes.findById(id)).contains(conMiniatura);
        assertThat(imagenes.findById(id)).get().extracting(Imagen::rutaMiniatura).isEqualTo(Imagen.rutaMiniatura(ruta));
        assertThat(imagenes.findById(antigua.id())).get().extracting(Imagen::urlMiniatura).isNull();
    }

    @Test
    void findsSimilarImagesByHammingDistanceOfTheirHashes() {
        Imagen pendiente = imagenes.saveAndFlush(imagen(EstadoImagen.PENDIENTE, AHORA));
        Imagen casiIgual = imagenes.saveAndFlush(imagen(EstadoImagen.APROBADA, AHORA.plusSeconds(1)));
        Imagen identica = imagenes.saveAndFlush(imagen(EstadoImagen.RECHAZADA, AHORA.plusSeconds(2)));
        Imagen distinta = imagenes.saveAndFlush(imagen(EstadoImagen.APROBADA, AHORA.plusSeconds(3)));
        Imagen sinHuella = imagenes.saveAndFlush(imagen(EstadoImagen.APROBADA, AHORA.plusSeconds(4)));
        long huella = 0x0123_4567_89AB_CDEFL;
        imagenes.guardarHuella(pendiente.id(), huella);
        imagenes.guardarHuella(casiIgual.id(), huella ^ 0b1011);
        imagenes.guardarHuella(identica.id(), huella);
        // También funciona con el bit de signo: la huella es un long de 64 bits completos
        imagenes.guardarHuella(distinta.id(), ~huella);

        assertThat(imagenes.findParecidas(pendiente.id(), 6, 10)).containsExactly(identica, casiIgual);
        assertThat(imagenes.findParecidas(pendiente.id(), 6, 1)).containsExactly(identica);
        assertThat(imagenes.findParecidas(pendiente.id(), 2, 10)).containsExactly(identica);
        assertThat(imagenes.findParecidas(distinta.id(), 6, 10)).isEmpty();
        assertThat(imagenes.findParecidas(sinHuella.id(), 64, 10)).isEmpty();
    }

    @Test
    void moderationQueueAndDeletion() {
        long pendientesAntes = imagenes.countByEstado(EstadoImagen.PENDIENTE);
        Imagen pendiente = imagenes.save(imagen(EstadoImagen.PENDIENTE, Instant.parse("2000-01-01T00:00:00Z")));

        assertThat(imagenes.countByEstado(EstadoImagen.PENDIENTE)).isEqualTo(pendientesAntes + 1);
        assertThat(imagenes.findByEstado(EstadoImagen.PENDIENTE, PageQuery.first(1)).content())
                .containsExactly(pendiente);

        imagenes.save(pendiente.aprobar());
        assertThat(imagenes.findById(pendiente.id())).get().extracting(Imagen::estado).isEqualTo(EstadoImagen.APROBADA);

        imagenes.delete(pendiente.id());
        assertThat(imagenes.findById(pendiente.id())).isEmpty();
    }

    @Test
    void deletesEveryImageOfABenchIncludingRejectedOnes() {
        Imagen aprobada = imagenes.save(imagen(EstadoImagen.APROBADA, AHORA));
        Imagen rechazada = imagenes.save(imagen(EstadoImagen.RECHAZADA, AHORA.plusSeconds(1)));

        assertThat(imagenes.deleteByBanco(bancoId)).containsExactlyInAnyOrder(aprobada, rechazada);
        assertThat(imagenes.findById(aprobada.id())).isEmpty();
        assertThat(imagenes.findById(rechazada.id())).isEmpty();
        assertThat(imagenes.deleteByBanco(bancoId)).isEmpty();
    }

    private Imagen imagen(EstadoImagen estado, Instant fecha) {
        UUID id = UUID.randomUUID();
        return new Imagen(id, bancoId, "bancos/" + bancoId + "/" + id + ".jpg", "https://storage.test/" + id,
                "image/jpeg", 1234, "img-it", estado, fecha);
    }
}
