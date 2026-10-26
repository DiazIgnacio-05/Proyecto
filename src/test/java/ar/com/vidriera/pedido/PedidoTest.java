package ar.com.vidriera.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class PedidoTest {

    private final LineaPedido empanadas = new LineaPedido("Empanada de carne", 6, new BigDecimal("1800"));
    private final LineaPedido locro = new LineaPedido("Locro criollo", 1, new BigDecimal("12000"));

    @Test
    void elTotalSumaLosSubtotales() {
        Pedido pedido = new Pedido("Sabores", List.of(empanadas, locro), DatosEntrega.retiro("Ana", null));
        assertThat(empanadas.subtotal()).isEqualByComparingTo("10800");
        assertThat(pedido.total()).isEqualByComparingTo("22800");
    }

    @Test
    void elTotalRespetaLosCentavos() {
        LineaPedido conCentavos = new LineaPedido("Alfajor", 3, new BigDecimal("1250.50"));
        Pedido pedido = new Pedido("Sabores", List.of(conCentavos), DatosEntrega.retiro("Ana", null));
        assertThat(pedido.total()).isEqualByComparingTo("3751.50");
    }

    @Test
    void elMensajeEnMesaListaProductosTotalYMesa() {
        Pedido pedido = new Pedido("Sabores del Valle", List.of(empanadas, locro),
                DatosEntrega.enMesa(5, null, "Sin cebolla"));
        assertThat(MensajeWhatsApp.de(pedido)).isEqualTo("""
                ¡Hola Sabores del Valle! Quiero hacer este pedido:

                • 6 x Empanada de carne — $ 10.800
                • 1 x Locro criollo — $ 12.000

                *Total: $ 22.800*

                *Mesa 5*
                Comentario: Sin cebolla""");
    }

    @Test
    void elMensajeDeEnvioIncluyeNombreYDireccion() {
        Pedido pedido = new Pedido("Sabores", List.of(locro),
                DatosEntrega.envio("  Juan   Pérez ", "Av. Belgrano 1250, depto 3", ""));
        assertThat(MensajeWhatsApp.de(pedido))
                .contains("*Envío a domicilio:* Av. Belgrano 1250, depto 3")
                .contains("Nombre: Juan Pérez")
                .doesNotContain("Comentario");
    }

    @Test
    void elRetiroRequiereNombre() {
        assertThatThrownBy(() -> DatosEntrega.retiro("   ", null))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void elEnvioRequiereDireccion() {
        assertThatThrownBy(() -> DatosEntrega.envio("Juan", null, null))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessageContaining("dirección");
    }

    @Test
    void enMesaElNombreEsOpcionalPeroLaMesaNo() {
        assertThat(DatosEntrega.enMesa(3, null, null).nombre()).isNull();
        assertThatThrownBy(() -> new DatosEntrega(ModalidadPedido.MESA, null, null, null, null))
                .isInstanceOf(PedidoInvalidoException.class);
    }

    @Test
    void rechazaPedidosVaciosOConCantidadesFueraDeRango() {
        assertThatThrownBy(() -> new Pedido("Sabores", List.of(), DatosEntrega.retiro("Ana", null)))
                .isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> new LineaPedido("Empanada", 0, BigDecimal.ONE))
                .isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> new LineaPedido("Empanada", 100, BigDecimal.ONE))
                .isInstanceOf(PedidoInvalidoException.class);
    }

    @Test
    void rechazaPedidosConDemasiadasLineas() {
        List<LineaPedido> muchas = Collections.nCopies(Pedido.LINEAS_MAXIMAS + 1, locro);
        assertThatThrownBy(() -> new Pedido("Sabores", muchas, DatosEntrega.retiro("Ana", null)))
                .isInstanceOf(PedidoInvalidoException.class);
    }
}
