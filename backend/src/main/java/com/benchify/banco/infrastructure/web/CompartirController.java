package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.ConsultarBancosUseCase;
import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.Caracteristicas;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EntradaSitemap;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lo que leen los buscadores y las redes sociales, que no ejecutan la aplicación Angular. Firebase
 * Hosting envía aquí {@code /s/**}, {@code /sitemap.xml} y {@code /robots.txt}.
 *
 * <p>{@code /s/{id}} es el enlace que se comparte: una página mínima con las etiquetas Open Graph
 * del banco que lleva a la ficha en el acto. Los bots se quedan con las etiquetas; las personas,
 * con la ficha.
 */
@RestController
@Hidden
class CompartirController {

    private static final MediaType HTML = new MediaType("text", "html", StandardCharsets.UTF_8);
    private static final int MAX_DESCRIPCION = 200;

    private final ConsultarBancosUseCase bancos;
    private final ImagenesBancoUseCase imagenes;
    private final String web;

    CompartirController(ConsultarBancosUseCase bancos, ImagenesBancoUseCase imagenes, WebProperties web) {
        this.bancos = bancos;
        this.imagenes = imagenes;
        this.web = web.url();
    }

    @GetMapping("/s/{id}")
    ResponseEntity<String> compartir(@PathVariable UUID id) {
        String ficha = web + "/bancos/" + id;
        Banco banco;
        try {
            banco = bancos.obtener(id, null);
        } catch (BancoNotFoundException e) {
            // La ficha ya explica que no existe (o que aún no se ha publicado)
            return ResponseEntity.status(302).location(URI.create(ficha)).build();
        }
        DatosBanco datos = banco.datos();
        String titulo = datos.titulo() + " · Benchify";
        String html = """
                <!doctype html>
                <html lang="es">
                <head>
                <meta charset="utf-8">
                <title>%1$s</title>
                <meta name="description" content="%2$s">
                <link rel="canonical" href="%3$s">
                <meta property="og:type" content="website">
                <meta property="og:site_name" content="Benchify">
                <meta property="og:locale" content="es_ES">
                <meta property="og:title" content="%1$s">
                <meta property="og:description" content="%2$s">
                <meta property="og:url" content="%3$s">
                <meta property="og:image" content="%4$s">
                <meta property="place:location:latitude" content="%5$s">
                <meta property="place:location:longitude" content="%6$s">
                <meta name="twitter:card" content="summary_large_image">
                <meta http-equiv="refresh" content="0; url=%3$s">
                </head>
                <body><p><a href="%3$s">Ver «%7$s» en Benchify</a></p></body>
                </html>
                """.formatted(attr(titulo), attr(descripcion(banco)), attr(ficha), attr(imagen(banco)),
                datos.ubicacion().lat(), datos.ubicacion().lng(), HtmlUtils.htmlEscape(datos.titulo()));
        return ResponseEntity.ok()
                .contentType(HTML)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(html);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        xml.append("  <url><loc>").append(web).append("/mapa</loc></url>\n");
        for (EntradaSitemap entrada : bancos.sitemap()) {
            xml.append("  <url><loc>").append(web).append("/bancos/").append(entrada.bancoId())
                    .append("</loc><lastmod>").append(entrada.actualizado().atOffset(ZoneOffset.UTC).toLocalDate())
                    .append("</lastmod></url>\n");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(xml.toString());
    }

    /**
     * Las zonas privadas no aportan nada a un buscador. {@code /s/} sí se permite: algunos bots de
     * redes sociales respetan robots.txt y sin acceso no mostrarían la vista previa; su enlace
     * canónico apunta a la ficha, así que no cuenta como contenido duplicado.
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> robots() {
        String robots = """
                User-agent: *
                Disallow: /admin
                Disallow: /perfil
                Disallow: /login
                Disallow: /registro
                Disallow: /bancos/nuevo

                Sitemap: %s/sitemap.xml
                """.formatted(web);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic()).body(robots);
    }

    /** La descripción del autor o, si no hay, un resumen con la ciudad, la valoración y lo que ofrece. */
    static String descripcion(Banco banco) {
        DatosBanco datos = banco.datos();
        if (datos.descripcion() != null) {
            return recortar(datos.descripcion());
        }
        List<String> partes = new ArrayList<>();
        partes.add(datos.direccion().ciudad() != null ? "Banco en " + datos.direccion().ciudad() : "Banco público");
        if (banco.valoracion().numResenas() > 0) {
            partes.add(String.format(Locale.forLanguageTag("es"), "%.1f ★ (%d %s)", banco.valoracion().media(),
                    banco.valoracion().numResenas(), banco.valoracion().numResenas() == 1 ? "reseña" : "reseñas"));
        }
        Caracteristicas c = datos.caracteristicas();
        String extras = Stream.of(c.accesible() ? "accesible" : null, c.conSombra() ? "con sombra" : null,
                        c.conRespaldo() ? "con respaldo" : null)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        if (!extras.isEmpty()) {
            partes.add(extras);
        }
        return String.join(" · ", partes);
    }

    /** La primera foto publicada del banco o, si no tiene, la imagen genérica de la web. */
    private String imagen(Banco banco) {
        return imagenes.listar(banco.id(), null).stream()
                .filter(i -> i.estado() == EstadoImagen.APROBADA)
                .map(Imagen::url)
                .findFirst()
                .orElse(web + "/og.png");
    }

    private static String recortar(String texto) {
        return texto.length() <= MAX_DESCRIPCION ? texto : texto.substring(0, MAX_DESCRIPCION - 1).strip() + "…";
    }

    private static String attr(String valor) {
        return HtmlUtils.htmlEscape(valor, "UTF-8");
    }
}
