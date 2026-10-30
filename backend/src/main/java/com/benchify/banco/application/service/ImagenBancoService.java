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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Procesar una foto y subirla al bucket lleva segundos: se hace fuera de cualquier transacción para
 * no retener una conexión del pool, y solo el registro de la fila es transaccional. Los ficheros se
 * borran después del commit: si la transacción se deshace, la fila sigue apuntando a ellos.
 */
@Service
public class ImagenBancoService implements ImagenesBancoUseCase {

    private final BancoRepository bancos;
    private final ImagenRepository imagenes;
    private final AlmacenImagenesPort almacen;
    private final ProcesadorImagenesPort procesador;
    private final BorradoFicherosImagen ficheros;
    private final TransactionTemplate transaccion;
    private final TransactionTemplate lectura;
    private final Clock clock;

    public ImagenBancoService(BancoRepository bancos, ImagenRepository imagenes, AlmacenImagenesPort almacen,
            ProcesadorImagenesPort procesador, BorradoFicherosImagen ficheros,
            PlatformTransactionManager transacciones, Clock clock) {
        this.bancos = bancos;
        this.imagenes = imagenes;
        this.almacen = almacen;
        this.procesador = procesador;
        this.ficheros = ficheros;
        this.transaccion = new TransactionTemplate(transacciones);
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
        this.clock = clock;
    }

    @Override
    public Imagen subir(Actor actor, UUID bancoId, ArchivoImagen archivo) {
        // Comprobación previa sin bloqueo: evita procesar y subir una foto que se va a rechazar
        lectura.executeWithoutResult(status -> exigirHueco(admiteFotos(bancos.findById(bancoId), bancoId, actor)));

        ImagenProcesada procesada = procesador.procesar(archivo);
        UUID id = UUID.randomUUID();
        String ruta = Imagen.rutaPara(bancoId, id);
        FicheroAlmacenado foto = guardar(ruta, procesada.contenido());
        FicheroAlmacenado miniatura;
        try {
            miniatura = guardar(Imagen.rutaMiniatura(ruta), procesada.miniatura());
        } catch (RuntimeException e) {
            ficheros.borrarYa(List.of(ruta));
            throw e;
        }
        Imagen nueva = Imagen.nueva(id, bancoId, foto, miniatura, procesada.tamanoBytes(), actor, clock.instant());
        try {
            // El commit ocurre dentro del try: si falla, los ficheros también se borran
            return transaccion.execute(status -> registrar(nueva, actor));
        } catch (RuntimeException e) {
            // Sin fila en la base de datos los ficheros quedarían huérfanos en el bucket
            ficheros.borrarYa(nueva.rutas());
            throw e;
        }
    }

    /**
     * Repite las comprobaciones con la fila del banco bloqueada: dos subidas simultáneas no pueden
     * superar el máximo de fotos ni colarse en un banco que se está eliminando.
     */
    private Imagen registrar(Imagen nueva, Actor actor) {
        Banco banco = admiteFotos(bancos.findByIdForUpdate(nueva.bancoId()), nueva.bancoId(), actor);
        exigirHueco(banco);
        return imagenes.saveAndFlush(nueva);
    }

    private FicheroAlmacenado guardar(String ruta, byte[] contenido) {
        return new FicheroAlmacenado(ruta, almacen.guardar(ruta, contenido, ImagenProcesada.CONTENT_TYPE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Imagen> listar(UUID bancoId, Actor actor) {
        bancoVisible(bancos.findById(bancoId), bancoId, actor);
        return imagenes.findActivasByBanco(bancoId).stream()
                .filter(imagen -> imagen.esVisiblePara(actor))
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(Actor actor, UUID imagenId) {
        Imagen imagen = cargarBloqueada(imagenId);
        String autorBanco = bancos.findById(imagen.bancoId()).map(b -> b.autor().id()).orElse(null);
        if (!actor.canManage(imagen.subidaPor()) && !actor.userId().equals(autorBanco)) {
            throw new ForbiddenException("IMAGEN_NO_AUTOR", "No puedes borrar esta imagen");
        }
        imagenes.delete(imagenId);
        ficheros.borrarTrasCommit(imagen.rutas());
    }

    @Override
    @Transactional
    public Imagen aprobar(Actor actor, UUID imagenId) {
        exigirAdmin(actor);
        return imagenes.save(cargarBloqueada(imagenId).aprobar());
    }

    @Override
    @Transactional
    public void rechazar(Actor actor, UUID imagenId) {
        exigirAdmin(actor);
        Imagen imagen = cargarBloqueada(imagenId);
        imagenes.save(imagen.rechazar());
        // La imagen ya no es visible; si el fichero no se puede borrar, queda huérfano pero no bloquea la moderación
        ficheros.borrarTrasCommit(imagen.rutas());
    }

    /** La fila bloqueada garantiza que se modera o se borra con las rutas que tiene de verdad. */
    private Imagen cargarBloqueada(UUID imagenId) {
        return imagenes.findByIdForUpdate(imagenId).orElseThrow(() -> new ImagenNotFoundException(imagenId));
    }

    /** Un banco sin publicar solo es visible para su autor y los administradores: nadie más puede subir fotos. */
    private static Banco admiteFotos(Optional<Banco> banco, UUID bancoId, Actor actor) {
        Banco visible = bancoVisible(banco, bancoId, actor);
        visible.exigirNoEliminado();
        return visible;
    }

    private static Banco bancoVisible(Optional<Banco> banco, UUID bancoId, Actor actor) {
        return banco.filter(b -> b.esVisiblePara(actor)).orElseThrow(() -> new BancoNotFoundException(bancoId));
    }

    private void exigirHueco(Banco banco) {
        if (imagenes.countActivasByBanco(banco.id()) >= Imagen.MAX_POR_BANCO) {
            throw new BusinessRuleException("BANCO_MAX_IMAGENES",
                    "Un banco admite como máximo " + Imagen.MAX_POR_BANCO + " fotos");
        }
    }

    private static void exigirAdmin(Actor actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new ForbiddenException("ADMIN_REQUIRED", "Solo un administrador puede moderar imágenes");
        }
    }
}
