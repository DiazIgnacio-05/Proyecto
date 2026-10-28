package ar.com.vidriera.catalogo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import ar.com.vidriera.tenant.EntidadDelComercio;

@Entity
public class Producto extends EntidadDelComercio {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    /** Precio en pesos. {@code BigDecimal} evita los errores de redondeo de {@code double}. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** {@code false} = "sin stock": se muestra pero no se puede pedir. */
    private boolean disponible = true;

    private boolean destacado;

    private int orden;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<ImagenProducto> imagenes = new ArrayList<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Variante> variantes = new ArrayList<>();

    protected Producto() {
    }

    public Producto(Categoria categoria, String nombre, BigDecimal precio) {
        setCategoria(categoria);
        setNombre(nombre);
        setPrecio(precio);
    }

    public ImagenProducto agregarImagen(String clave) {
        ImagenProducto imagen = new ImagenProducto(this, clave, imagenes.size());
        imagenes.add(imagen);
        return imagen;
    }

    /** Quita la foto (se borra de la base por orphanRemoval) y renumera el orden de las demás. */
    public Optional<ImagenProducto> quitarImagen(Long imagenId) {
        Optional<ImagenProducto> imagen = imagenes.stream().filter(i -> i.getId().equals(imagenId)).findFirst();
        imagen.ifPresent(i -> {
            imagenes.remove(i);
            for (int orden = 0; orden < imagenes.size(); orden++) {
                imagenes.get(orden).setOrden(orden);
            }
        });
        return imagen;
    }

    /** Pone la foto indicada primera (la que se ve en el listado). */
    public void hacerPrincipal(Long imagenId) {
        imagenes.stream().filter(i -> i.getId().equals(imagenId)).findFirst().ifPresent(principal -> {
            imagenes.remove(principal);
            imagenes.addFirst(principal);
            for (int orden = 0; orden < imagenes.size(); orden++) {
                imagenes.get(orden).setOrden(orden);
            }
        });
    }

    public static final int VARIANTES_MAXIMAS = 60;

    public Variante agregarVariante(String talle, String color) {
        if (variantes.size() >= VARIANTES_MAXIMAS) {
            throw new IllegalStateException("Un producto puede tener hasta " + VARIANTES_MAXIMAS + " variantes");
        }
        Variante variante = new Variante(this, talle, color);
        variantes.add(variante);
        return variante;
    }

    /** Agrega la combinación talle/color si todavía no existe. Devuelve si la agregó. */
    public boolean agregarVarianteSiNoExiste(String talle, String color) {
        if (variantes.stream().anyMatch(v -> v.esCombinacion(talle, color))) {
            return false;
        }
        agregarVariante(talle, color);
        return true;
    }

    public Optional<Variante> buscarVariante(Long varianteId) {
        return variantes.stream().filter(v -> v.getId() != null && v.getId().equals(varianteId)).findFirst();
    }

    public Optional<Variante> quitarVariante(Long varianteId) {
        Optional<Variante> variante = buscarVariante(varianteId);
        variante.ifPresent(variantes::remove);
        return variante;
    }

    public boolean tieneVariantes() {
        return !variantes.isEmpty();
    }

    public Optional<ImagenProducto> getImagenPrincipal() {
        return imagenes.stream().findFirst();
    }

    /** Se puede pedir: está disponible y, si tiene variantes, al menos una lo está. */
    public boolean sePuedePedir() {
        return disponible && (variantes.isEmpty() || variantes.stream().anyMatch(Variante::isDisponible));
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = Objects.requireNonNull(categoria, "categoria");
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El producto necesita un nombre");
        }
        this.nombre = nombre.trim();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser mayor o igual a cero");
        }
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

    public List<ImagenProducto> getImagenes() {
        return Collections.unmodifiableList(imagenes);
    }

    public List<Variante> getVariantes() {
        return Collections.unmodifiableList(variantes);
    }
}
