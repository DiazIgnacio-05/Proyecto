package ar.com.vidriera.pedido;

import ar.com.vidriera.comun.FormatoPesos;

/**
 * Arma el texto que le llega al comercio por WhatsApp. WhatsApp muestra en negrita lo que va
 * entre asteriscos ({@code *Total*}).
 */
public final class MensajeWhatsApp {

    private MensajeWhatsApp() {
    }

    public static String de(Pedido pedido) {
        StringBuilder texto = new StringBuilder();
        texto.append("¡Hola ").append(pedido.nombreComercio()).append("! Quiero hacer este pedido:\n\n");

        for (LineaPedido linea : pedido.lineas()) {
            texto.append("• ").append(linea.cantidad()).append(" x ").append(linea.descripcion())
                    .append(" — ").append(FormatoPesos.formatear(linea.subtotal())).append('\n');
        }
        texto.append("\n*Total: ").append(FormatoPesos.formatear(pedido.total())).append("*\n\n");

        DatosEntrega entrega = pedido.entrega();
        switch (entrega.modalidad()) {
            case MESA -> texto.append("*Mesa ").append(entrega.mesa()).append("*\n");
            case RETIRO -> texto.append("*Retiro en el local*\n");
            case ENVIO -> texto.append("*Envío a domicilio:* ").append(entrega.direccion()).append('\n');
        }
        if (entrega.nombre() != null) {
            texto.append("Nombre: ").append(entrega.nombre()).append('\n');
        }
        if (entrega.comentario() != null) {
            texto.append("Comentario: ").append(entrega.comentario()).append('\n');
        }
        return texto.toString().stripTrailing();
    }
}
