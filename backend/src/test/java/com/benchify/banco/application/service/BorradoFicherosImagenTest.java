package com.benchify.banco.application.service;

import com.benchify.banco.application.port.out.AlmacenImagenesPort;
import com.benchify.support.RecordingTransactionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BorradoFicherosImagenTest {

    private static final List<String> RUTAS = List.of("bancos/a/1.jpg", "bancos/a/1_thumb.jpg");

    @Mock
    private AlmacenImagenesPort almacen;

    private final RecordingTransactionManager transacciones = new RecordingTransactionManager();

    @Test
    void withoutATransactionFilesAreDeletedRightAway() {
        new BorradoFicherosImagen(almacen).borrarTrasCommit(RUTAS);

        verify(almacen).borrar("bancos/a/1.jpg");
        verify(almacen).borrar("bancos/a/1_thumb.jpg");
    }

    @Test
    void insideATransactionFilesAreDeletedOnlyAfterTheCommit() {
        BorradoFicherosImagen borrado = new BorradoFicherosImagen(almacen);

        new TransactionTemplate(transacciones).executeWithoutResult(status -> {
            borrado.borrarTrasCommit(RUTAS);
            verifyNoInteractions(almacen);
        });

        verify(almacen).borrar("bancos/a/1.jpg");
        verify(almacen).borrar("bancos/a/1_thumb.jpg");
    }

    @Test
    void aRolledBackTransactionKeepsTheFiles() {
        BorradoFicherosImagen borrado = new BorradoFicherosImagen(almacen);

        new TransactionTemplate(transacciones).executeWithoutResult(status -> {
            borrado.borrarTrasCommit(RUTAS);
            status.setRollbackOnly();
        });

        verifyNoInteractions(almacen);
    }

    @Test
    void storageFailuresAreLoggedAndTheRemainingFilesAreStillDeleted() {
        doThrow(new IllegalStateException("bucket down")).when(almacen).borrar("bancos/a/1.jpg");

        assertThatCode(() -> new BorradoFicherosImagen(almacen).borrarYa(RUTAS)).doesNotThrowAnyException();

        verify(almacen).borrar("bancos/a/1_thumb.jpg");
    }
}
