package com.benchify.banco.application.service;

import com.benchify.banco.application.port.out.AlmacenImagenesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * Borra ficheros de fotos del almacenamiento sin que un fallo del bucket rompa la operación: un
 * fichero huérfano es preferible a deshacer un cambio que la base de datos ya ha aceptado.
 */
@Component
public class BorradoFicherosImagen {

    private static final Logger log = LoggerFactory.getLogger(BorradoFicherosImagen.class);

    private final AlmacenImagenesPort almacen;

    public BorradoFicherosImagen(AlmacenImagenesPort almacen) {
        this.almacen = almacen;
    }

    /** Borra ya los ficheros; p. ej., los recién subidos cuya fila no se ha llegado a guardar. */
    public void borrarYa(Collection<String> rutas) {
        rutas.forEach(this::borrarSinFallar);
    }

    /**
     * Borra los ficheros cuando la transacción en curso confirme. Si se deshace, la fila sigue
     * apuntando a ellos y no se tocan. Sin transacción en curso se borran en el acto.
     */
    public void borrarTrasCommit(Collection<String> rutas) {
        List<String> pendientes = List.copyOf(rutas);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            borrarYa(pendientes);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                borrarYa(pendientes);
            }
        });
    }

    private void borrarSinFallar(String ruta) {
        try {
            almacen.borrar(ruta);
        } catch (RuntimeException e) {
            log.warn("Could not delete stored image {}", ruta, e);
        }
    }
}
