package ar.com.vidriera.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.com.vidriera.comercio.HorarioAtencion;
import ar.com.vidriera.comercio.HorarioAtencionRepository;
import ar.com.vidriera.tenant.TenantContext;

/**
 * El dueño del comercio A intenta ver, modificar y borrar datos del comercio B usando ids de B.
 * Todo tiene que fallar con 404 (como si no existieran) y los datos de B no se tienen que tocar.
 */
class AislamientoDelPanelTest extends PruebaDelPanel {

    @Autowired
    private HorarioAtencionRepository horarios;

    private ComercioDePrueba a;
    private ComercioDePrueba b;

    @BeforeEach
    void crearDosComercios() {
        a = crearComercio("a");
        b = crearComercio("b");
    }

    @Test
    void elListadoSoloMuestraLosProductosPropios() throws Exception {
        TenantContext.ejecutarComo(b.id(), () -> {
            var p = productos.findById(b.productoId()).orElseThrow();
            p.setNombre("Producto secreto de B");
            return productos.save(p);
        });
        mvc.perform(get("/admin/productos").header("Host", HOST_PANEL).with(comoDueno(a)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Producto secreto de B"))));
    }

    @Test
    void noPuedeVerNiEditarProductosDeOtroComercio() throws Exception {
        mvc.perform(get("/admin/productos/{id}", b.productoId()).header("Host", HOST_PANEL).with(comoDueno(a)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/admin/productos/{id}", b.productoId()).header("Host", HOST_PANEL)
                        .with(comoDueno(a)).with(csrf())
                        .param("categoriaId", a.categoriaId().toString())
                        .param("nombre", "Hackeado").param("precio", "1"))
                .andExpect(status().isNotFound());
        assertThat(producto(b, b.productoId()).getNombre()).isEqualTo("Muzzarella");
    }

    @Test
    void noPuedeBorrarNiCambiarStockDeProductosDeOtroComercio() throws Exception {
        mvc.perform(post("/admin/productos/{id}/eliminar", b.productoId()).header("Host", HOST_PANEL)
                        .with(comoDueno(a)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/admin/productos/{id}/disponible", b.productoId()).header("Host", HOST_PANEL)
                        .with(comoDueno(a)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(producto(b, b.productoId())).isNotNull().matches(p -> p.isDisponible());
    }

    @Test
    void noPuedeCrearUnProductoEnUnaCategoriaDeOtroComercio() throws Exception {
        mvc.perform(post("/admin/productos").header("Host", HOST_PANEL).with(comoDueno(a)).with(csrf())
                        .param("categoriaId", b.categoriaId().toString())
                        .param("nombre", "Intruso").param("precio", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La categoría elegida no existe")));
        assertThat(TenantContext.ejecutarComo(b.id(), () -> productos.count())).isEqualTo(1);
    }

    @Test
    void noPuedeTocarCategoriasDeOtroComercio() throws Exception {
        mvc.perform(post("/admin/categorias/{id}", b.categoriaId()).header("Host", HOST_PANEL)
                        .with(comoDueno(a)).with(csrf())
                        .param("nombre", "Hackeada").param("orden", "1"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/admin/categorias/{id}/eliminar", b.categoriaId()).header("Host", HOST_PANEL)
                        .with(comoDueno(a)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(TenantContext.ejecutarComo(b.id(), () -> categorias.findById(b.categoriaId())))
                .hasValueSatisfying(c -> assertThat(c.getNombre()).isEqualTo("Pizzas"));
    }

    @Test
    void guardarLosHorariosDeUnComercioNoBorraLosDeOtro() throws Exception {
        TenantContext.ejecutarComo(b.id(), () ->
                horarios.save(new HorarioAtencion(java.time.DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))));

        mvc.perform(post("/admin/horarios").header("Host", HOST_PANEL).with(comoDueno(a)).with(csrf())
                        .param("dias[0].dia", "MONDAY").param("dias[0].apertura1", "10:00").param("dias[0].cierre1", "14:00"))
                .andExpect(status().is3xxRedirection());

        assertThat(TenantContext.ejecutarComo(b.id(), () -> horarios.count())).isEqualTo(1);
        assertThat(TenantContext.ejecutarComo(a.id(), () -> horarios.findAll()))
                .singleElement()
                .matches(h -> h.getFranja().apertura().equals(LocalTime.of(10, 0)));
    }

    @Test
    void losDatosDelComercioQueSeEditanSonLosDelUsuarioLogueado() throws Exception {
        mvc.perform(post("/admin/comercio").header("Host", HOST_PANEL).with(comoDueno(a)).with(csrf())
                        .param("nombre", "Nuevo nombre de A").param("whatsapp", "383 4222222")
                        .param("colorPrimario", "#112233").param("colorSecundario", "#445566"))
                .andExpect(status().is3xxRedirection());
        assertThat(comercios.findById(a.id()).orElseThrow().getNombre()).isEqualTo("Nuevo nombre de A");
        assertThat(comercios.findById(b.id()).orElseThrow().getNombre()).isNotEqualTo("Nuevo nombre de A");
    }
}
