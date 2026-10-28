package ar.com.vidriera.admin;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.vidriera.catalogo.Categoria;
import ar.com.vidriera.catalogo.CategoriaRepository;
import ar.com.vidriera.catalogo.ImagenProducto;
import ar.com.vidriera.catalogo.Producto;
import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.catalogo.Variante;
import ar.com.vidriera.comun.LectorPesos;
import ar.com.vidriera.imagen.ServicioImagenes;
import ar.com.vidriera.imagen.TamanoImagen;

/** Productos del comercio logueado: listado, alta, edición, stock, fotos y baja. */
@Service
public class ProductoAdminService {

    static final int FOTOS_MAXIMAS = 5;

    /**
     * @param variantes          cantidad de talles/colores cargados (0 si no tiene)
     * @param variantesConStock  cuántos de ellos están disponibles
     */
    public record ProductoFila(Long id, String nombre, BigDecimal precio, boolean disponible,
            boolean destacado, String imagenUrl, int variantes, int variantesConStock) {
    }

    public record VarianteFila(Long id, String etiqueta, boolean disponible) {
    }

    public record Seccion(Long id, String nombre, boolean activa, List<ProductoFila> productos) {
    }

    public record FotoFila(Long id, String url) {
    }

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final ServicioImagenes imagenes;

    public ProductoAdminService(ProductoRepository productos, CategoriaRepository categorias,
            ServicioImagenes imagenes) {
        this.productos = productos;
        this.categorias = categorias;
        this.imagenes = imagenes;
    }

    /** Todas las categorías (también las vacías o inactivas) con sus productos. */
    @Transactional(readOnly = true)
    public List<Seccion> listar() {
        Map<Long, List<ProductoFila>> porCategoria = productos.findAllByOrderByOrdenAscNombreAsc().stream()
                .collect(Collectors.groupingBy(p -> p.getCategoria().getId(),
                        Collectors.mapping(this::aFila, Collectors.toList())));
        return categorias.findAllByOrderByOrdenAscNombreAsc().stream()
                .map(c -> new Seccion(c.getId(), c.getNombre(), c.isActiva(),
                        porCategoria.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoForm formulario(Long id) {
        Producto producto = buscar(id);
        ProductoForm form = new ProductoForm();
        form.setId(producto.getId());
        form.setCategoriaId(producto.getCategoria().getId());
        form.setNombre(producto.getNombre());
        form.setDescripcion(producto.getDescripcion());
        form.setPrecio(LectorPesos.paraEditar(producto.getPrecio()));
        form.setDisponible(producto.isDisponible());
        form.setDestacado(producto.isDestacado());
        form.setOrden(producto.getOrden());
        return form;
    }

    @Transactional(readOnly = true)
    public List<FotoFila> fotos(Long id) {
        return buscar(id).getImagenes().stream()
                .map(i -> new FotoFila(i.getId(), imagenes.urlFoto(i.getClave(), TamanoImagen.CHICA)))
                .toList();
    }

    /** Crea o actualiza el producto y devuelve su id. */
    @Transactional
    public Long guardar(ProductoForm form) {
        BigDecimal precio = LectorPesos.leer(form.getPrecio()).orElseThrow(() -> new OperacionRechazadaException(
                "No entendimos el precio \"" + form.getPrecio() + "\". Escribilo así: 12500, 12.500 o 1.250,50"));
        // La categoría se busca en el comercio actual: una categoría ajena "no existe".
        Categoria categoria = categorias.findById(form.getCategoriaId())
                .orElseThrow(() -> new OperacionRechazadaException("La categoría elegida no existe"));

        Producto producto = form.esNuevo()
                ? new Producto(categoria, form.getNombre(), precio)
                : buscar(form.getId());
        producto.setCategoria(categoria);
        producto.setNombre(form.getNombre());
        producto.setPrecio(precio);
        producto.setDescripcion(textoOpcional(form.getDescripcion()));
        producto.setDisponible(form.isDisponible());
        producto.setDestacado(form.isDestacado());
        producto.setOrden(form.getOrden());
        return productos.save(producto).getId();
    }

    /** Marca o desmarca "sin stock" con un toque desde el listado. */
    @Transactional
    public boolean cambiarDisponible(Long id) {
        Producto producto = buscar(id);
        producto.setDisponible(!producto.isDisponible());
        return producto.isDisponible();
    }

    @Transactional
    public String eliminar(Long id) {
        Producto producto = buscar(id);
        List<String> claves = producto.getImagenes().stream().map(ImagenProducto::getClave).toList();
        productos.delete(producto);
        AlConfirmar.ejecutar(() -> claves.forEach(imagenes::eliminarFoto));
        return producto.getNombre();
    }

    @Transactional
    public void agregarFoto(Long id, byte[] archivo) {
        Producto producto = buscar(id);
        if (producto.getImagenes().size() >= FOTOS_MAXIMAS) {
            throw new OperacionRechazadaException("Cada producto puede tener hasta " + FOTOS_MAXIMAS + " fotos");
        }
        String clave = imagenes.guardarFotoDeProducto(producto.getComercioId(), archivo);
        producto.agregarImagen(clave);
    }

    @Transactional
    public void quitarFoto(Long id, Long fotoId) {
        ImagenProducto foto = buscar(id).quitarImagen(fotoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Foto"));
        AlConfirmar.ejecutar(() -> imagenes.eliminarFoto(foto.getClave()));
    }

    @Transactional
    public void hacerPrincipal(Long id, Long fotoId) {
        buscar(id).hacerPrincipal(fotoId);
    }

    @Transactional(readOnly = true)
    public List<VarianteFila> variantes(Long id) {
        return buscar(id).getVariantes().stream()
                .sorted(Comparator.comparing(Variante::getId))
                .map(v -> new VarianteFila(v.getId(), v.etiqueta(), v.isDisponible()))
                .toList();
    }

    /**
     * Crea todas las combinaciones de los talles y colores escritos ("S, M, L" y "Negro, Blanco"
     * → 6 variantes). Las que ya existen se saltean. Devuelve cuántas se agregaron.
     */
    @Transactional
    public int agregarVariantes(Long id, String talles, String colores) {
        List<String> listaTalles = lista(talles, 20, "talle");
        List<String> listaColores = lista(colores, 30, "color");
        if (listaTalles.isEmpty() && listaColores.isEmpty()) {
            throw new OperacionRechazadaException("Escribí al menos un talle o un color");
        }
        Producto producto = buscar(id);
        List<String> ts = listaTalles.isEmpty() ? Collections.singletonList(null) : listaTalles;
        List<String> cs = listaColores.isEmpty() ? Collections.singletonList(null) : listaColores;
        if (producto.getVariantes().size() + ts.size() * cs.size() > Producto.VARIANTES_MAXIMAS) {
            throw new OperacionRechazadaException(
                    "Son demasiadas combinaciones: un producto puede tener hasta " + Producto.VARIANTES_MAXIMAS);
        }
        int agregadas = 0;
        for (String talle : ts) {
            for (String color : cs) {
                if (producto.agregarVarianteSiNoExiste(talle, color)) {
                    agregadas++;
                }
            }
        }
        return agregadas;
    }

    /** Marca o desmarca "sin stock" un talle/color puntual. */
    @Transactional
    public boolean cambiarVariante(Long id, Long varianteId) {
        Variante variante = buscar(id).buscarVariante(varianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Variante"));
        variante.setDisponible(!variante.isDisponible());
        return variante.isDisponible();
    }

    @Transactional
    public void quitarVariante(Long id, Long varianteId) {
        buscar(id).quitarVariante(varianteId).orElseThrow(() -> new RecursoNoEncontradoException("Variante"));
    }

    private Producto buscar(Long id) {
        return productos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto"));
    }

    /** "S, M ,  L" → [S, M, L], sin repetidos (sin importar mayúsculas). */
    static List<String> lista(String texto, int largoMaximo, String que) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        Map<String, String> unicos = new LinkedHashMap<>();
        for (String parte : texto.split("[,;]")) {
            String limpio = parte.strip().replaceAll("\\s+", " ");
            if (limpio.isEmpty()) {
                continue;
            }
            if (limpio.length() > largoMaximo) {
                throw new OperacionRechazadaException(
                        "El " + que + " \"" + limpio + "\" es demasiado largo (máx. " + largoMaximo + ")");
            }
            unicos.putIfAbsent(limpio.toLowerCase(), limpio);
        }
        return List.copyOf(unicos.values());
    }

    private ProductoFila aFila(Producto p) {
        String imagen = p.getImagenPrincipal()
                .map(i -> imagenes.urlFoto(i.getClave(), TamanoImagen.CHICA))
                .orElse(null);
        int conStock = (int) p.getVariantes().stream().filter(Variante::isDisponible).count();
        return new ProductoFila(p.getId(), p.getNombre(), p.getPrecio(), p.isDisponible(), p.isDestacado(), imagen,
                p.getVariantes().size(), conStock);
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
