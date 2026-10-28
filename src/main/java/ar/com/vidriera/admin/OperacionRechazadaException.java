package ar.com.vidriera.admin;

/** Algo que el dueño intentó hacer y no se puede; el mensaje se le muestra tal cual. */
public class OperacionRechazadaException extends RuntimeException {

    public OperacionRechazadaException(String mensaje) {
        super(mensaje);
    }
}
