package ar.com.vidriera.superadmin;

/** Una operación del super-admin que no se puede hacer; el mensaje se muestra tal cual. */
public class AltaRechazadaException extends RuntimeException {

    public AltaRechazadaException(String mensaje) {
        super(mensaje);
    }
}
