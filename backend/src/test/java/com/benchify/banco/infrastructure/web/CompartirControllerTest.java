package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.ConsultarBancosUseCase;
import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.Caracteristicas;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.DireccionPostal;
import com.benchify.banco.domain.EntradaSitemap;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.banco.domain.Valoracion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ApiControllerTest(CompartirController.class)
@Import(WebConfig.class)
@TestPropertySource(properties = "benchify.web.url=https://benchify.web.app/")
class CompartirControllerTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T10:00:00Z");

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private ConsultarBancosUseCase bancos;
    @MockitoBean
    private ImagenesBancoUseCase imagenes;

    @Test
    void theSharedLinkCarriesOpenGraphTagsAndLeadsToTheBench() {
        Banco banco = publicado(new DatosBanco("Banco <con> \"comillas\"", "Vistas al río & a Triana",
                new Ubicacion(37.38, -5.99), null, null, null));
        when(bancos.obtener(banco.id(), null)).thenReturn(banco);
        Imagen foto = new Imagen(UUID.randomUUID(), banco.id(), "bancos/f.jpg", "https://storage.test/f.jpg",
                "image/jpeg", 10, "ana", EstadoImagen.APROBADA, AHORA);
        when(imagenes.listar(banco.id(), null)).thenReturn(List.of(foto));

        String ficha = "https://benchify.web.app/bancos/" + banco.id();
        assertThat(mvc.get().uri("/s/{id}", banco.id()))
                .hasStatusOk()
                .hasContentTypeCompatibleWith("text/html")
                .hasHeader("Cache-Control", "max-age=600, public")
                .bodyText()
                .contains("<meta property=\"og:title\" content=\"Banco &lt;con&gt; &quot;comillas&quot; · Benchify\">")
                .contains("<meta property=\"og:description\" content=\"Vistas al río &amp; a Triana\">")
                .contains("<meta property=\"og:image\" content=\"https://storage.test/f.jpg\">")
                .contains("<meta property=\"og:url\" content=\"" + ficha + "\">")
                .contains("<meta http-equiv=\"refresh\" content=\"0; url=" + ficha + "\">")
                .doesNotContain("<script");
    }

    @Test
    void withoutPhotosOrDescriptionItUsesTheGenericImageAndASummary() {
        Banco banco = publicado(new DatosBanco("Glorieta", null, new Ubicacion(37.38, -5.99),
                new DireccionPostal(null, "Sevilla", null, null), new Caracteristicas(true, true, false, null), null));
        banco.actualizarValoracion(new Valoracion(4.5, 2));
        when(bancos.obtener(banco.id(), null)).thenReturn(banco);
        when(imagenes.listar(banco.id(), null)).thenReturn(List.of());

        assertThat(mvc.get().uri("/s/{id}", banco.id()))
                .hasStatusOk()
                .bodyText()
                .contains("content=\"https://benchify.web.app/og.png\"")
                .contains("content=\"Banco en Sevilla · 4,5 ★ (2 reseñas) · accesible, con sombra\"");
    }

    @Test
    void anUnknownOrUnpublishedBenchGoesStraightToTheApp() {
        UUID id = UUID.randomUUID();
        when(bancos.obtener(id, null)).thenThrow(new BancoNotFoundException(id));

        assertThat(mvc.get().uri("/s/{id}", id))
                .hasStatus(302)
                .hasHeader("Location", "https://benchify.web.app/bancos/" + id);
    }

    @Test
    void sitemapListsPublishedBenchesAndRobotsPointsToIt() {
        UUID id = UUID.randomUUID();
        when(bancos.sitemap()).thenReturn(List.of(new EntradaSitemap(id, Instant.parse("2026-10-30T23:30:00Z"))));

        assertThat(mvc.get().uri("/sitemap.xml"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith("application/xml")
                .bodyText()
                .contains("<loc>https://benchify.web.app/mapa</loc>")
                .contains("<url><loc>https://benchify.web.app/bancos/" + id + "</loc><lastmod>2026-10-30</lastmod></url>");
        assertThat(mvc.get().uri("/robots.txt"))
                .hasStatusOk()
                .bodyText()
                .contains("Disallow: /admin")
                .contains("Sitemap: https://benchify.web.app/sitemap.xml");
    }

    private static Banco publicado(DatosBanco datos) {
        Banco banco = Banco.proponer(UUID.randomUUID(), datos, Actor.user("ana"), AHORA);
        banco.aprobar(AHORA);
        return banco;
    }
}
