package ar.com.vidriera.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.com.vidriera.catalogo.Variante;
import ar.com.vidriera.comercio.Rubro;

/** Talles y colores desde el panel de una tienda de ropa. */
class VariantesDelPanelTest extends PruebaDelPanel {

    @Test
    void creaTodasLasCombinacionesSinRepetir() throws Exception {
        ComercioDePrueba c = crearComercio("ropa", Rubro.ROPA);
        mvc.perform(post("/admin/productos/{id}/variantes", c.productoId()).header("Host", HOST_PANEL)
                        .with(comoDueno(c)).with(csrf())
                        .param("talles", "S, M, L").param("colores", "Negro, Blanco"))
                .andExpect(flash().attribute("ok", "Se agregaron 6 combinaciones"));

        mvc.perform(post("/admin/productos/{id}/variantes", c.productoId()).header("Host", HOST_PANEL)
                        .with(comoDueno(c)).with(csrf())
                        .param("talles", "m, XL").param("colores", "negro"))
                .andExpect(flash().attribute("ok", "Se agregó 1 combinación"));

        assertThat(etiquetas(c)).hasSize(7).contains("talle XL, negro", "talle S, Blanco");
    }

    @Test
    void marcaUnTalleSinStockYLoElimina() throws Exception {
        ComercioDePrueba c = crearComercio("ropa", Rubro.ROPA);
        mvc.perform(post("/admin/productos/{id}/variantes", c.productoId()).header("Host", HOST_PANEL)
                .with(comoDueno(c)).with(csrf()).param("talles", "M"));
        Long varianteId = enTransaccion(c, () -> productos.findById(c.productoId()).orElseThrow()
                .getVariantes().getFirst().getId());

        mvc.perform(post("/admin/productos/{id}/variantes/{v}/disponible", c.productoId(), varianteId)
                        .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()))
                .andExpect(flash().attribute("ok", "Marcado sin stock"));
        assertThat(enTransaccion(c, () -> productos.findById(c.productoId()).orElseThrow().sePuedePedir())).isFalse();

        mvc.perform(post("/admin/productos/{id}/variantes/{v}/eliminar", c.productoId(), varianteId)
                        .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()))
                .andExpect(flash().attribute("ok", "Combinación eliminada"));
        assertThat(etiquetas(c)).isEmpty();
    }

    @Test
    void noPuedeTocarVariantesDeOtroComercio() throws Exception {
        ComercioDePrueba a = crearComercio("a", Rubro.ROPA);
        ComercioDePrueba b = crearComercio("b", Rubro.ROPA);
        mvc.perform(post("/admin/productos/{id}/variantes", b.productoId()).header("Host", HOST_PANEL)
                        .with(comoDueno(a)).with(csrf()).param("talles", "M"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/admin/productos/{id}/variantes", b.productoId()).header("Host", HOST_PANEL)
                .with(comoDueno(b)).with(csrf()).param("talles", "M"));
        Long varianteDeB = enTransaccion(b, () -> productos.findById(b.productoId()).orElseThrow()
                .getVariantes().getFirst().getId());

        // Aunque use su propio producto en la URL, la variante de B no aparece en él.
        mvc.perform(post("/admin/productos/{id}/variantes/{v}/eliminar", a.productoId(), varianteDeB)
                        .header("Host", HOST_PANEL).with(comoDueno(a)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(etiquetas(b)).containsExactly("talle M");
    }

    @Test
    void laSeccionDeTallesSoloApareceEnRubrosConVariantes() throws Exception {
        ComercioDePrueba ropa = crearComercio("ropa", Rubro.ROPA);
        ComercioDePrueba resto = crearComercio("resto", Rubro.RESTAURANTE);
        mvc.perform(get("/admin/productos/{id}", ropa.productoId()).header("Host", HOST_PANEL).with(comoDueno(ropa)))
                .andExpect(content().string(containsString("Talles y colores")));
        mvc.perform(get("/admin/productos/{id}", resto.productoId()).header("Host", HOST_PANEL).with(comoDueno(resto)))
                .andExpect(content().string(not(containsString("Talles y colores"))));
    }

    private List<String> etiquetas(ComercioDePrueba c) {
        return enTransaccion(c, () -> productos.findById(c.productoId()).orElseThrow()
                .getVariantes().stream().map(Variante::etiqueta).toList());
    }
}
