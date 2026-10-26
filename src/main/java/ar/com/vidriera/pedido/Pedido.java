package ar.com.vidriera.pedido;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Pedido armado y validado, listo para convertirse en mensaje de WhatsApp. No se guarda en la base. */
public record Pedido(String nombreComercio, List<LineaPedido> lineas, DatosEntrega entrega) {

    public static final int LINEAS_MAXIMAS = 50;

    public Pedido {
        Objects.requireNonNull(nombreComercio, "nombreComercio");
        Objects.requireNonNull(entrega, "entrega");
        if (lineas == null || lineas.isEmpty()) {
            throw new PedidoInvalidoException("El pedido está vacío");
        }
        if (lineas.size() > LINEAS_MAXIMAS) {
            throw new PedidoInvalidoException("El pedido tiene demasiados productos");
        }
        lineas = List.copyOf(lineas);
    }

    public BigDecimal total() {
        return lineas.stream().map(LineaPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
