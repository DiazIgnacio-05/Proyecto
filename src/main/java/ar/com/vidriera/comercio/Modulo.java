package ar.com.vidriera.comercio;

/**
 * Módulos adicionales que un comercio puede contratar aparte del abono básico.
 * El código consulta {@code comercio.tieneModulo(...)} para habilitar cada función.
 */
public enum Modulo {

    DOMINIO_PROPIO("Dominio propio"),
    ESTADISTICAS("Estadísticas de visitas y pedidos"),
    PEDIDOS_REGISTRADOS("Pedidos registrados en el panel"),
    PAGOS_ONLINE("Cobro online con Mercado Pago"),
    RESERVAS("Reservas de mesa");

    private final String etiqueta;

    Modulo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
