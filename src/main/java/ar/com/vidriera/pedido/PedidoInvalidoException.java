package ar.com.vidriera.pedido;

/** El pedido no se puede armar; el mensaje se le muestra al cliente tal cual. */
public class PedidoInvalidoException extends RuntimeException {

    public PedidoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
