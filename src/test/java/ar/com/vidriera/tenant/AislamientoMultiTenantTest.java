package ar.com.vidriera.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ar.com.vidriera.PruebaDeIntegracion;
import ar.com.vidriera.catalogo.Categoria;
import ar.com.vidriera.catalogo.CategoriaRepository;
import ar.com.vidriera.catalogo.Producto;
import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.Rubro;

/**
 * Verifica el requisito de seguridad más importante: un comercio nunca ve ni modifica datos de otro.
 */
class AislamientoMultiTenantTest extends PruebaDeIntegracion {

    @Autowired
    private ComercioRepository comercios;
    @Autowired
    private CategoriaRepository categorias;
    @Autowired
    private ProductoRepository productos;

    private Long comercioA;
    private Long comercioB;
    private Producto productoDeA;
    private Producto productoDeB;
    private Categoria categoriaDeB;

    @BeforeEach
    void crearDosComercios() {
        comercioA = crearComercio("a").getId();
        comercioB = crearComercio("b").getId();

        productoDeA = TenantContext.ejecutarComo(comercioA, () -> {
            Categoria categoria = categorias.save(new Categoria("Pizzas", 1));
            return productos.save(new Producto(categoria, "Muzzarella", new BigDecimal("9000")));
        });
        categoriaDeB = TenantContext.ejecutarComo(comercioB, () -> categorias.save(new Categoria("Remeras", 1)));
        productoDeB = TenantContext.ejecutarComo(comercioB,
                () -> productos.save(new Producto(categoriaDeB, "Remera lisa", new BigDecimal("14000"))));
    }

    @AfterEach
    void borrarComercios() {
        TenantContext.limpiar();
        // ON DELETE CASCADE borra categorías y productos de cada comercio.
        comercios.deleteAllById(java.util.List.of(comercioA, comercioB));
    }

    @Test
    void alGuardarSeAsignaElComercioActualAutomaticamente() {
        assertThat(productoDeA.getComercioId()).isEqualTo(comercioA);
        assertThat(productoDeB.getComercioId()).isEqualTo(comercioB);
    }

    @Test
    void cadaComercioSoloListaSusProductos() {
        assertThat(TenantContext.ejecutarComo(comercioA, () -> productos.findAll()))
                .extracting(Producto::getNombre)
                .containsExactly("Muzzarella");
        assertThat(TenantContext.ejecutarComo(comercioB, () -> productos.findAll()))
                .extracting(Producto::getNombre)
                .containsExactly("Remera lisa");
    }

    @Test
    void buscarPorIdUnProductoDeOtroComercioNoLoEncuentra() {
        assertThat(TenantContext.ejecutarComo(comercioA, () -> productos.findById(productoDeB.getId())))
                .isEmpty();
        assertThat(TenantContext.ejecutarComo(comercioA, () -> productos.existsById(productoDeB.getId())))
                .isFalse();
    }

    @Test
    void noSePuedeBorrarUnProductoDeOtroComercio() {
        TenantContext.ejecutarComo(comercioA, () -> {
            productos.deleteById(productoDeB.getId());
            return null;
        });
        assertThat(TenantContext.ejecutarComo(comercioB, () -> productos.existsById(productoDeB.getId())))
                .isTrue();
    }

    @Test
    void laBaseRechazaUsarUnaCategoriaDeOtroComercio() {
        // Aunque alguien consiguiera una categoría ajena, la clave foránea compuesta lo impide.
        assertThatThrownBy(() -> TenantContext.ejecutarComo(comercioA,
                () -> productos.save(new Producto(categoriaDeB, "Intruso", BigDecimal.ONE))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sinComercioEnElContextoNoSeVeNingunDato() {
        assertThat(productos.findById(productoDeA.getId())).isEmpty();
        assertThat(productos.findAll())
                .extracting(Producto::getComercioId)
                .doesNotContain(comercioA, comercioB);
    }

    @Test
    void elModoSistemaVeLosDatosDeTodosLosComercios() {
        assertThat(TenantContext.ejecutarComoSistema(() -> productos.findAll()))
                .extracting(Producto::getComercioId)
                .contains(comercioA, comercioB);
    }

    private Comercio crearComercio(String prefijo) {
        String slug = prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
        return comercios.save(new Comercio(slug, "Comercio " + slug, Rubro.RESTAURANTE, "3834111111",
                LocalDate.now()));
    }
}
