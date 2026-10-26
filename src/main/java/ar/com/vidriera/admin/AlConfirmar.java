package ar.com.vidriera.admin;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ejecuta algo recién cuando la transacción actual se confirmó en la base (commit). Se usa para
 * borrar archivos de imagen: si la transacción fallara, la base seguiría apuntando a archivos que
 * todavía existen.
 */
final class AlConfirmar {

    private AlConfirmar() {
    }

    static void ejecutar(Runnable accion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
