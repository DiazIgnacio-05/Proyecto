package ar.com.vidriera.pedido;

import java.math.BigDecimal;
import java.util.Objects;

/** Un renglón del pedido: qué, cuántos y a qué precio (el precio sale de la base, no del navegador). */
public record LineaPedido(String descripcion, int cantidad, BigDecimal precioUnitario) {

    public static final int CANTIDAD_MAXIMA = 99;

    public LineaPedido {
        Objects.requireNonNull(descripcion, "descripcion");
        Objects.requireNonNull(precioUnitario, "precioUnitario");
        if (cantidad < 1 || cantidad > CANTIDAD_MAXIMA) {
            throw new PedidoInvalidoException("La cantidad debe estar entre 1 y " + CANTIDAD_MAXIMA);
        }
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
