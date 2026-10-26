package ar.com.vidriera.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import ar.com.vidriera.tenant.EntidadDelComercio;

/**
 * Foto de un producto. Se guarda solo la "clave" del archivo; las URLs de cada tamaño
 * (chico para listados, grande para el detalle) las arma el servicio de imágenes.
 */
@Entity
public class ImagenProducto extends EntidadDelComercio {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private String clave;

    private int orden;

    protected ImagenProducto() {
    }

    ImagenProducto(Producto producto, String clave, int orden) {
        this.producto = producto;
        this.clave = clave;
        this.orden = orden;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getClave() {
        return clave;
    }

    public int getOrden() {
        return orden;
    }
}
