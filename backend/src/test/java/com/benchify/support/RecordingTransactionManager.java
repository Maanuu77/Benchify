package com.benchify.support;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * Gestor de transacciones sin base de datos para los tests unitarios: activa la sincronización
 * (los callbacks {@code afterCommit} se ejecutan de verdad), cuenta commits y rollbacks y puede
 * simular un fallo al confirmar las que escriben.
 */
public class RecordingTransactionManager extends AbstractPlatformTransactionManager {

    private int commits;
    private int rollbacks;
    private boolean fallarAlConfirmar;

    /** Las transacciones de solo lectura siguen confirmando. */
    public void fallarAlConfirmarEscrituras() {
        this.fallarAlConfirmar = true;
    }

    public int commits() {
        return commits;
    }

    public int rollbacks() {
        return rollbacks;
    }

    @Override
    protected Object doGetTransaction() {
        return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        // Nada que abrir
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        if (fallarAlConfirmar && !status.isReadOnly()) {
            throw new TransactionSystemException("Simulated commit failure");
        }
        commits++;
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        rollbacks++;
    }
}
