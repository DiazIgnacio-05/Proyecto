package ar.com.vidriera.catalogo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma el catálogo (o menú) del comercio actual para el sitio público. Devuelve records simples en
 * lugar de entidades: la vista no puede disparar consultas "lazy" fuera de la transacción.
 */
@Service
public class CatalogoService {

    public record ProductoVista(Long id, String nombre, String descripcion, BigDecimal precio,
            boolean sePuedePedir, boolean destacado) {
    }

    public record Seccion(Long id, String nombre, List<ProductoVista> productos) {
    }

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public CatalogoService(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    /** Categorías activas con productos, en el orden que eligió el comercio. */
    @Transactional(readOnly = true)
    public List<Seccion> secciones() {
        Map<Long, List<ProductoVista>> porCategoria = productos.findAllByOrderByOrdenAscNombreAsc().stream()
                .collect(Collectors.groupingBy(p -> p.getCategoria().getId(),
                        Collectors.mapping(CatalogoService::aVista, Collectors.toList())));

        return categorias.findAllByOrderByOrdenAscNombreAsc().stream()
                .filter(Categoria::isActiva)
                .filter(categoria -> porCategoria.containsKey(categoria.getId()))
                .map(categoria -> new Seccion(categoria.getId(), categoria.getNombre(),
                        porCategoria.get(categoria.getId())))
                .toList();
    }

    private static ProductoVista aVista(Producto p) {
        return new ProductoVista(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.sePuedePedir(), p.isDestacado());
    }
}
