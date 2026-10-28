package ar.com.vidriera.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import ar.com.vidriera.tenant.EntidadDelComercio;

/** Combinación talle/color de un producto (rubro ropa). Permite marcar, por ejemplo, "M agotado". */
@Entity
public class Variante extends EntidadDelComercio {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    private String talle;

    private String color;

    private boolean disponible = true;

    protected Variante() {
    }

    Variante(Producto producto, String talle, String color) {
        if ((talle == null || talle.isBlank()) && (color == null || color.isBlank())) {
            throw new IllegalArgumentException("La variante necesita al menos talle o color");
        }
        this.producto = producto;
        this.talle = limpiar(talle);
        this.color = limpiar(color);
    }

    /** Texto para el cliente y para el mensaje de WhatsApp: "talle M, Negro". */
    public String etiqueta() {
        if (talle != null && color != null) {
            return "talle " + talle + ", " + color;
        }
        return talle != null ? "talle " + talle : color;
    }

    /** Misma combinación de talle y color, sin importar mayúsculas. */
    public boolean esCombinacion(String otroTalle, String otroColor) {
        return iguales(talle, limpiar(otroTalle)) && iguales(color, limpiar(otroColor));
    }

    private static boolean iguales(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    public Producto getProducto() {
        return producto;
    }

    public String getTalle() {
        return talle;
    }

    public String getColor() {
        return color;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
}
