package com.benchify.banco.application.service;

import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.application.port.out.AlmacenImagenesPort;
import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.ImagenRepository;
import com.benchify.banco.application.port.out.ProcesadorImagenesPort;
import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.FicheroAlmacenado;
import com.benchify.banco.domain.Imagen;
import com.benchify.banco.domain.ImagenNotFoundException;
import com.benchify.banco.domain.ImagenProcesada;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ImagenBancoService implements ImagenesBancoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImagenBancoService.class);

    private final BancoRepository bancos;
    private final ImagenRepository imagenes;
    private final AlmacenImagenesPort almacen;
    private final ProcesadorImagenesPort procesador;
    private final Clock clock;

    public ImagenBancoService(BancoRepository bancos, ImagenRepository imagenes, AlmacenImagenesPort almacen,
            ProcesadorImagenesPort procesador, Clock clock) {
        this.bancos = bancos;
        this.imagenes = imagenes;
        this.almacen = almacen;
        this.procesador = procesador;
        this.clock = clock;
    }

    @Override
    public Imagen subir(Actor actor, UUID bancoId, ArchivoImagen archivo) {
        // Un banco sin publicar solo es visible para su autor y los administradores: nadie más puede subir fotos
        bancoVisible(bancoId, actor);
        if (imagenes.countActivasByBanco(bancoId) >= Imagen.MAX_POR_BANCO) {
            throw new BusinessRuleException("BANCO_MAX_IMAGENES",
                    "Un banco admite como máximo " + Imagen.MAX_POR_BANCO + " fotos");
        }

        ImagenProcesada procesada = procesador.procesar(archivo);
        UUID id = UUID.randomUUID();
        String ruta = Imagen.rutaPara(bancoId, id);
        FicheroAlmacenado foto = guardar(ruta, procesada.contenido());
        FicheroAlmacenado miniatura;
        try {
            miniatura = guardar(Imagen.rutaMiniatura(ruta), procesada.miniatura());
        } catch (RuntimeException e) {
            borrarSinFallar(ruta);
            throw e;
        }
        Imagen nueva = Imagen.nueva(id, bancoId, foto, miniatura, procesada.tamanoBytes(), actor, clock.instant());
        try {
            return imagenes.save(nueva);
        } catch (RuntimeException e) {
            // Sin fila en la base de datos los ficheros quedarían huérfanos en el bucket
            nueva.rutas().forEach(this::borrarSinFallar);
            throw e;
        }
    }

    private FicheroAlmacenado guardar(String ruta, byte[] contenido) {
        return new FicheroAlmacenado(ruta, almacen.guardar(ruta, contenido, ImagenProcesada.CONTENT_TYPE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Imagen> listar(UUID bancoId, Actor actor) {
        bancoVisible(bancoId, actor);
        return imagenes.findActivasByBanco(bancoId).stream()
                .filter(imagen -> imagen.esVisiblePara(actor))
                .toList();
    }

    @Override
    public void eliminar(Actor actor, UUID imagenId) {
        Imagen imagen = imagenes.findById(imagenId).orElseThrow(() -> new ImagenNotFoundException(imagenId));
        String autorBanco = bancos.findById(imagen.bancoId()).map(b -> b.autor().id()).orElse(null);
        if (!actor.canManage(imagen.subidaPor()) && !actor.userId().equals(autorBanco)) {
            throw new ForbiddenException("IMAGEN_NO_AUTOR", "No puedes borrar esta imagen");
        }
        imagenes.delete(imagenId);
        imagen.rutas().forEach(this::borrarSinFallar);
    }

    private Banco bancoVisible(UUID bancoId, Actor actor) {
        return bancos.findById(bancoId)
                .filter(banco -> banco.esVisiblePara(actor))
                .orElseThrow(() -> new BancoNotFoundException(bancoId));
    }

    private void borrarSinFallar(String ruta) {
        try {
            almacen.borrar(ruta);
        } catch (RuntimeException e) {
            log.warn("Could not delete stored image {}", ruta, e);
        }
    }
}
