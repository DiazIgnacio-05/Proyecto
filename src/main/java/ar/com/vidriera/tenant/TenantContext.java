package ar.com.vidriera.tenant;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Guarda el comercio (tenant) con el que se está trabajando en el hilo actual.
 *
 * <p>Usa un {@link ThreadLocal}: cada request HTTP se atiende en su propio hilo, así que dos
 * requests de comercios distintos nunca se pisan. Quien establece el comercio es responsable de
 * limpiarlo al terminar (los hilos se reutilizan entre requests).
 *
 * <p>Importante: Hibernate lee este valor al <b>abrir</b> la sesión (al comenzar la transacción
 * o la llamada al repositorio). Cambiarlo en medio de una transacción no tiene efecto.
 */
public final class TenantContext {

    /** Valor especial: acceso a los datos de todos los comercios. Solo para tareas del sistema. */
    static final long SISTEMA = -1L;

    /** Valor especial: ningún comercio. Las consultas sobre datos de comercios no devuelven nada. */
    static final long NINGUNO = 0L;

    private static final ThreadLocal<Long> ACTUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void establecer(Long comercioId) {
        Objects.requireNonNull(comercioId, "comercioId");
        if (comercioId <= 0) {
            throw new IllegalArgumentException("Id de comercio inválido: " + comercioId);
        }
        ACTUAL.set(comercioId);
    }

    public static Optional<Long> actual() {
        Long id = ACTUAL.get();
        return id == null || id == SISTEMA ? Optional.empty() : Optional.of(id);
    }

    public static void limpiar() {
        ACTUAL.remove();
    }

    /** Ejecuta la tarea como el comercio indicado y restaura el valor anterior al terminar. */
    public static <T> T ejecutarComo(Long comercioId, Supplier<T> tarea) {
        Long anterior = ACTUAL.get();
        establecer(comercioId);
        try {
            return tarea.get();
        } finally {
            restaurar(anterior);
        }
    }

    /**
     * Ejecuta la tarea con acceso a los datos de todos los comercios (por ejemplo, el alta de un
     * comercio desde el panel de super-admin). Usar con cuidado.
     */
    public static <T> T ejecutarComoSistema(Supplier<T> tarea) {
        Long anterior = ACTUAL.get();
        ACTUAL.set(SISTEMA);
        try {
            return tarea.get();
        } finally {
            restaurar(anterior);
        }
    }

    /** Valor que ve Hibernate: el comercio actual, {@link #SISTEMA} o {@link #NINGUNO}. */
    static long paraHibernate() {
        Long id = ACTUAL.get();
        return id == null ? NINGUNO : id;
    }

    private static void restaurar(Long anterior) {
        if (anterior == null) {
            ACTUAL.remove();
        } else {
            ACTUAL.set(anterior);
        }
    }
}
