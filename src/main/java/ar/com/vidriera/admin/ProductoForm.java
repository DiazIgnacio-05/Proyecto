package ar.com.vidriera.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulario de alta y edición de producto. El precio llega como texto ("12.500") y lo interpreta
 * {@link ar.com.vidriera.comun.LectorPesos}. No tiene campo de comercio: lo define el usuario logueado.
 */
public class ProductoForm {

    private Long id;

    @NotNull(message = "Elegí una categoría")
    private Long categoriaId;

    @NotBlank(message = "Escribí el nombre del producto")
    @Size(max = 120, message = "El nombre es demasiado largo (máx. 120)")
    private String nombre;

    @Size(max = 1000, message = "La descripción es demasiado larga (máx. 1000)")
    private String descripcion;

    @NotBlank(message = "Escribí el precio")
    private String precio;

    private boolean disponible = true;

    private boolean destacado;

    @Min(value = 0, message = "El orden no puede ser negativo")
    @Max(value = 9999, message = "El orden es demasiado grande")
    private int orden;

    public boolean esNuevo() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public boolean isDestacado() {
        return destacado;
    }

    public void setDestacado(boolean destacado) {
        this.destacado = destacado;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
