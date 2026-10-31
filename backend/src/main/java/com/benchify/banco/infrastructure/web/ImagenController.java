package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.banco.infrastructure.web.dto.ImagenResponse;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Imágenes", description = "Fotos de bancos en Firebase Cloud Storage")
class ImagenController {

    private static final Duration CACHE_PRIVADA = Duration.ofMinutes(5);

    private final ImagenesBancoUseCase imagenes;
    private final BancoWebMapper mapper;

    ImagenController(ImagenesBancoUseCase imagenes, BancoWebMapper mapper) {
        this.imagenes = imagenes;
        this.mapper = mapper;
    }

    @PostMapping(path = "/api/v1/bancos/{bancoId}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir una foto (JPEG, PNG o WebP, máximo 10 MB)",
            description = "Se guarda como JPEG sin metadatos (EXIF, GPS), con el lado mayor limitado a 2048 px y "
                    + "con una miniatura. Queda pendiente de moderación salvo que la suba un administrador; mientras "
                    + "tanto no es pública y sus url apuntan a la API (requiereAutenticacion).")
    @SecurityRequirement(name = "firebase")
    ResponseEntity<ImagenResponse> subir(@CurrentActor Actor actor, @PathVariable UUID bancoId,
            @RequestPart("archivo") MultipartFile archivo) {
        Imagen imagen = imagenes.subir(actor, bancoId, archivo(archivo));
        // El recurso creado es la imagen en la API; sus ficheros están en url y thumbUrl
        URI location = URI.create(ImagenUrls.api(ImagenUrls.RECURSO, imagen));
        return ResponseEntity.created(location).body(mapper.toResponse(imagen));
    }

    @GetMapping("/api/v1/bancos/{bancoId}/imagenes")
    @Operation(summary = "Fotos de un banco", description = "Aprobadas, más las pendientes que haya subido quien consulta.")
    List<ImagenResponse> listar(@PathVariable UUID bancoId, @CurrentActor(required = false) Actor actor) {
        return imagenes.listar(bancoId, actor).stream().map(mapper::toResponse).toList();
    }

    @GetMapping(ImagenUrls.RECURSO)
    @Operation(summary = "Una foto", description = "Las pendientes solo las ven quien las subió y los administradores.")
    ImagenResponse obtener(@PathVariable UUID imagenId, @CurrentActor(required = false) Actor actor) {
        return mapper.toResponse(imagenes.obtener(imagenId, actor));
    }

    @GetMapping(ImagenUrls.ARCHIVO)
    @Operation(summary = "Descargar una foto",
            description = "Para las pendientes, que no son públicas en el almacenamiento: solo las ven quien las "
                    + "subió y los administradores. Las aprobadas también se pueden descargar desde su url pública.")
    ResponseEntity<byte[]> archivo(@PathVariable UUID imagenId, @CurrentActor(required = false) Actor actor) {
        return fichero(imagenes.leerFichero(imagenId, false, actor));
    }

    @GetMapping(ImagenUrls.MINIATURA)
    @Operation(summary = "Descargar la miniatura de una foto", description = "Mismas reglas de visibilidad que la foto.")
    ResponseEntity<byte[]> miniatura(@PathVariable UUID imagenId, @CurrentActor(required = false) Actor actor) {
        return fichero(imagenes.leerFichero(imagenId, true, actor));
    }

    @DeleteMapping(ImagenUrls.RECURSO)
    @Operation(summary = "Borrar una foto propia o de un banco propio")
    @SecurityRequirement(name = "firebase")
    ResponseEntity<Void> eliminar(@CurrentActor Actor actor, @PathVariable UUID imagenId) {
        imagenes.eliminar(actor, imagenId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Caché solo en el navegador de quien la pide (puede ser una foto pendiente) y por poco tiempo:
     * al aprobarla o rechazarla su fichero cambia de sitio o desaparece.
     */
    private static ResponseEntity<byte[]> fichero(ArchivoImagen fichero) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichero.contentType()))
                .cacheControl(CacheControl.maxAge(CACHE_PRIVADA).cachePrivate())
                .body(fichero.contenido());
    }

    private static ArchivoImagen archivo(MultipartFile archivo) {
        // El tamaño declarado se comprueba antes de cargar el fichero en memoria
        ArchivoImagen.comprobarTamano(archivo.getSize());
        try {
            return new ArchivoImagen(archivo.getContentType(), archivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded file", e);
        }
    }
}
