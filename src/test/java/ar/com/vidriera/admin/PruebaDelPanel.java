package ar.com.vidriera.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ar.com.vidriera.PruebaDeIntegracion;
import ar.com.vidriera.catalogo.Categoria;
import ar.com.vidriera.catalogo.CategoriaRepository;
import ar.com.vidriera.catalogo.Producto;
import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.Rubro;
import ar.com.vidriera.tenant.TenantContext;
import ar.com.vidriera.usuario.Usuario;
import ar.com.vidriera.usuario.UsuarioDetailsService;
import ar.com.vidriera.usuario.UsuarioRepository;

/**
 * Base de los tests del panel. Cada test crea comercios propios (con dueño, una categoría y un
 * producto) y los borra al terminar, así no depende de los datos demo ni los modifica.
 */
@AutoConfigureMockMvc
abstract class PruebaDelPanel extends PruebaDeIntegracion {

    static final String HOST_PANEL = "app.localhost";
    static final String PASSWORD = "clave-de-prueba-1";

    /** Un comercio armado para el test. */
    record ComercioDePrueba(Long id, String slug, String email, Long categoriaId, Long productoId) {
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ComercioRepository comercios;
    @Autowired
    protected UsuarioRepository usuarios;
    @Autowired
    protected CategoriaRepository categorias;
    @Autowired
    protected ProductoRepository productos;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected UsuarioDetailsService usuarioDetailsService;
    @Autowired
    protected org.springframework.transaction.support.TransactionTemplate transacciones;

    private final List<Long> creados = new ArrayList<>();

    protected ComercioDePrueba crearComercio(String prefijo) {
        String slug = prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
        Comercio comercio = new Comercio(slug, "Comercio " + slug, Rubro.RESTAURANTE, "3834111111", LocalDate.now());
        comercio.setCantidadMesas(4);
        comercio = comercios.save(comercio);
        creados.add(comercio.getId());
        String email = slug + "@prueba.test";
        usuarios.save(Usuario.duenoDe(comercio, email, passwordEncoder.encode(PASSWORD), "Dueño " + slug));

        Long comercioId = comercio.getId();
        Categoria categoria = TenantContext.ejecutarComo(comercioId, () -> categorias.save(new Categoria("Pizzas", 1)));
        Producto producto = TenantContext.ejecutarComo(comercioId,
                () -> productos.save(new Producto(categoria, "Muzzarella", new BigDecimal("9000"))));
        return new ComercioDePrueba(comercioId, slug, email, categoria.getId(), producto.getId());
    }

    /** Autentica el request como el dueño del comercio (sin pasar por el formulario de login). */
    protected RequestPostProcessor comoDueno(ComercioDePrueba comercio) {
        return user(usuarioDetailsService.loadUserByUsername(comercio.email()));
    }

    protected Producto producto(ComercioDePrueba comercio, Long id) {
        return TenantContext.ejecutarComo(comercio.id(), () -> productos.findById(id)).orElse(null);
    }

    /**
     * Ejecuta la consulta dentro de una transacción del comercio (para poder recorrer colecciones
     * "lazy" como las fotos). El tenant se fija ANTES de abrir la transacción.
     */
    protected <T> T enTransaccion(ComercioDePrueba comercio, java.util.function.Supplier<T> consulta) {
        return TenantContext.ejecutarComo(comercio.id(), () -> transacciones.execute(estado -> consulta.get()));
    }

    @AfterEach
    void borrarComercios() {
        TenantContext.limpiar();
        comercios.deleteAllById(creados); // ON DELETE CASCADE borra usuarios, categorías y productos
        creados.clear();
    }
}
