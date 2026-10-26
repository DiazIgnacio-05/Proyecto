package ar.com.vidriera.comercio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Textos para buscadores y para la vista previa al compartir el link (WhatsApp, redes).
 * Si están vacíos se usan el nombre y la descripción del comercio.
 */
@Embeddable
public class Seo {

    @Column(name = "seo_titulo")
    private String titulo;

    @Column(name = "seo_descripcion")
    private String descripcion;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
