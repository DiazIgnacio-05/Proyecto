package ar.com.vidriera.pedido;

public enum ModalidadPedido {

    /** El cliente está en el local y entró por el QR de su mesa. */
    MESA("Para la mesa"),
    RETIRO("Retiro en el local"),
    /** Solo si el comercio ofrece envíos. */
    ENVIO("Envío a domicilio");

    private final String etiqueta;

    ModalidadPedido(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
