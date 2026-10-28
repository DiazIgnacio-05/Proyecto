package ar.com.vidriera.catalogo;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.vidriera.imagen.ServicioImagenes;
import ar.com.vidriera.imagen.TamanoImagen;

/**
 * Arma el catálogo (o menú) del comercio actual para el sitio público. Devuelve records simples en
 * lugar de entidades: la vista no puede disparar consultas "lazy" fuera de la transacción.
 */
@Service
public class CatalogoService {

    /** Una combinación de talle/color que el cliente puede elegir. */
    public record VarianteVista(Long id, String etiqueta, boolean disponible) {
    }

    /**
     * @param imagenUrl foto principal en tamaño chico, o {@code null} si no tiene
     * @param variantes talles/colores (vacía si el producto no tiene)
     */
    public record ProductoVista(Long id, String nombre, String descripcion, BigDecimal precio,
            boolean sePuedePedir, boolean destacado, String imagenUrl, List<VarianteVista> variantes,
            String textoEleccion) {

        public boolean tieneVariantes() {
            return !variantes.isEmpty();
        }
    }

    public record Seccion(Long id, String nombre, List<ProductoVista> productos) {
    }

    /** Resultado del buscador: las secciones filtradas y el total de productos encontrados. */
    public record Busqueda(List<Seccion> secciones, int cantidad) {
    }

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final ServicioImagenes imagenes;

    public CatalogoService(CategoriaRepository categorias, ProductoRepository productos,
            ServicioImagenes imagenes) {
        this.categorias = categorias;
        this.productos = productos;
        this.imagenes = imagenes;
    }

    /** Categorías activas con productos, en el orden que eligió el comercio. */
    @Transactional(readOnly = true)
    public List<Seccion> secciones() {
        Map<Long, List<ProductoVista>> porCategoria = productos.findAllByOrderByOrdenAscNombreAsc().stream()
                .collect(Collectors.groupingBy(p -> p.getCategoria().getId(),
                        Collectors.mapping(this::aVista, Collectors.toList())));

        return categorias.findAllByOrderByOrdenAscNombreAsc().stream()
                .filter(Categoria::isActiva)
                .filter(categoria -> porCategoria.containsKey(categoria.getId()))
                .map(categoria -> new Seccion(categoria.getId(), categoria.getNombre(),
                        porCategoria.get(categoria.getId())))
                .toList();
    }

    /**
     * Filtra secciones ya cargadas por texto y/o categoría. Sin filtros devuelve todo.
     *
     * @param texto       lo que escribió el cliente (puede ser null)
     * @param categoriaId categoría elegida en los filtros (puede ser null)
     */
    public static Busqueda filtrar(List<Seccion> todas, String texto, Long categoriaId) {
        List<String> palabras = BuscadorCatalogo.palabras(texto);
        List<Seccion> secciones = todas.stream()
                .filter(s -> categoriaId == null || s.id().equals(categoriaId))
                .map(s -> new Seccion(s.id(), s.nombre(), s.productos().stream()
                        .filter(p -> BuscadorCatalogo.coincide(palabras, p.nombre(), p.descripcion(), s.nombre()))
                        .toList()))
                .filter(s -> !s.productos().isEmpty())
                .toList();
        int cantidad = secciones.stream().mapToInt(s -> s.productos().size()).sum();
        return new Busqueda(secciones, cantidad);
    }

    private ProductoVista aVista(Producto p) {
        String imagen = p.getImagenPrincipal()
                .map(i -> imagenes.urlFoto(i.getClave(), TamanoImagen.CHICA))
                .orElse(null);
        List<VarianteVista> variantes = p.getVariantes().stream()
                .sorted(Comparator.comparing(Variante::getId))
                .map(v -> new VarianteVista(v.getId(), v.etiqueta(), v.isDisponible()))
                .toList();
        return new ProductoVista(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.sePuedePedir(), p.isDestacado(), imagen, variantes, textoEleccion(p.getVariantes()));
    }

    /** Texto corto para el selector (tiene que entrar en una tarjeta angosta de celular). */
    static String textoEleccion(List<Variante> variantes) {
        boolean talles = variantes.stream().anyMatch(v -> v.getTalle() != null);
        boolean colores = variantes.stream().anyMatch(v -> v.getColor() != null);
        if (talles && colores) {
            return "Talle y color";
        }
        return talles ? "Elegí talle" : "Elegí color";
    }
}
