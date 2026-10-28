package ar.com.vidriera.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.vidriera.catalogo.Categoria;
import ar.com.vidriera.catalogo.CategoriaRepository;
import ar.com.vidriera.catalogo.ProductoRepository;

/**
 * Categorías del comercio logueado. Todas las consultas pasan por repositorios con {@code @TenantId}:
 * un id de categoría de otro comercio simplemente "no existe".
 */
@Service
public class CategoriaAdminService {

    public record CategoriaFila(Long id, String nombre, int orden, boolean activa, long productos) {
    }

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public CategoriaAdminService(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<CategoriaFila> listar() {
        Map<Long, Long> cantidades = productos.contarPorCategoria().stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1]));
        return categorias.findAllByOrderByOrdenAscNombreAsc().stream()
                .map(c -> new CategoriaFila(c.getId(), c.getNombre(), c.getOrden(), c.isActiva(),
                        cantidades.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public void crear(String nombre) {
        String limpio = nombreValido(nombre);
        if (categorias.existsByNombreIgnoreCase(limpio)) {
            throw new OperacionRechazadaException("Ya existe una categoría llamada \"" + limpio + "\"");
        }
        int ultimoOrden = categorias.findAllByOrderByOrdenAscNombreAsc().stream()
                .mapToInt(Categoria::getOrden).max().orElse(0);
        categorias.save(new Categoria(limpio, ultimoOrden + 1));
    }

    @Transactional
    public void actualizar(Long id, String nombre, int orden, boolean activa) {
        Categoria categoria = buscar(id);
        String limpio = nombreValido(nombre);
        if (categorias.existsByNombreIgnoreCaseAndIdNot(limpio, id)) {
            throw new OperacionRechazadaException("Ya existe una categoría llamada \"" + limpio + "\"");
        }
        categoria.setNombre(limpio);
        categoria.setOrden(orden);
        categoria.setActiva(activa);
    }

    /** Solo se borran categorías vacías: así nunca se pierden productos por accidente. */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productos.existsByCategoriaId(id)) {
            throw new OperacionRechazadaException("La categoría \"" + categoria.getNombre()
                    + "\" tiene productos. Movelos a otra categoría o borralos antes de eliminarla.");
        }
        categorias.delete(categoria);
    }

    @Transactional(readOnly = true)
    public List<Categoria> paraElegir() {
        return categorias.findAllByOrderByOrdenAscNombreAsc();
    }

    private Categoria buscar(Long id) {
        return categorias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Categoría"));
    }

    private static String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new OperacionRechazadaException("Escribí un nombre para la categoría");
        }
        String limpio = nombre.strip().replaceAll("\\s+", " ");
        if (limpio.length() > 80) {
            throw new OperacionRechazadaException("El nombre de la categoría es demasiado largo (máx. 80)");
        }
        return limpio;
    }
}
