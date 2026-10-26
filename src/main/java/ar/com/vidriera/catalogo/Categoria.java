package ar.com.vidriera.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import ar.com.vidriera.tenant.EntidadDelComercio;

@Entity
public class Categoria extends EntidadDelComercio {

    @Column(nullable = false)
    private String nombre;

    private int orden;

    private boolean activa = true;

    protected Categoria() {
    }

    public Categoria(String nombre, int orden) {
        setNombre(nombre);
        this.orden = orden;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La categoría necesita un nombre");
        }
        this.nombre = nombre.trim();
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
