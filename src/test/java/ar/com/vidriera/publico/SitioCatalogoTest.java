package ar.com.vidriera.publico;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.vidriera.PruebaDeIntegracion;
import ar.com.vidriera.catalogo.CategoriaRepository;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.tenant.TenantContext;

/** Catálogo con buscador, filtros y talles para los rubros ropa y artículos (datos demo). */
@AutoConfigureMockMvc
class SitioCatalogoTest extends PruebaDeIntegracion {

    private static final String NOGAL = "nogal.localhost";
    private static final String ALAMEDA = "la-alameda.localhost";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ComercioRepository comercios;
    @Autowired
    private CategoriaRepository categorias;

    @Test
    void laTiendaDeRopaMuestraLaGrillaConTallesYColores() throws Exception {
        mvc.perform(get("/").header("Host", NOGAL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"grilla\"")))
                .andExpect(content().string(containsString("Buscar productos")))
                .andExpect(content().string(containsString(">talle M, Blanco</option>")))
                // Talle agotado: se muestra pero no se puede elegir.
                .andExpect(content().string(containsString("disabled=\"disabled\">talle L, Blanco (sin stock)</option>")))
                // La campera tiene todos los talles agotados: no se ofrece.
                .andExpect(content().string(containsString("Campera de jean")));
    }

    @Test
    void buscaSinImportarMayusculasNiTildes() throws Exception {
        mvc.perform(get("/").param("q", "PAVA electrica").header("Host", ALAMEDA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 resultado para “PAVA electrica”")))
                .andExpect(content().string(containsString("Pava eléctrica con corte a 80 °C")))
                .andExpect(content().string(not(containsString("Termo de acero 1 L"))));
    }

    @Test
    void filtraPorCategoria() throws Exception {
        Long mate = categoria("la-alameda", "Mate");
        mvc.perform(get("/").param("categoria", mate.toString()).header("Host", ALAMEDA))
                .andExpect(content().string(containsString("Termo de acero 1 L")))
                .andExpect(content().string(containsString("Bombilla de alpaca")))
                .andExpect(content().string(not(containsString("Licuadora 600 W"))));
    }

    @Test
    void ignoraCategoriasQueNoSonDelComercio() throws Exception {
        Long categoriaDeOtro = categoria("nogal", "Remeras");
        mvc.perform(get("/").param("categoria", categoriaDeOtro.toString()).header("Host", ALAMEDA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Licuadora 600 W")))
                .andExpect(content().string(not(containsString("Remera"))));
        mvc.perform(get("/").param("categoria", "abc").header("Host", ALAMEDA)).andExpect(status().isOk());
    }

    @Test
    void sinResultadosLoDice() throws Exception {
        mvc.perform(get("/").param("q", "bicicleta").header("Host", ALAMEDA))
                .andExpect(content().string(containsString("0 resultados")))
                .andExpect(content().string(containsString("No encontramos productos")));
    }

    @Test
    void elRestauranteSigueMostrandoElMenuSinBuscador() throws Exception {
        mvc.perform(get("/").header("Host", "sabores-del-valle.localhost"))
                .andExpect(content().string(containsString("Locro criollo")))
                .andExpect(content().string(not(containsString("Buscar productos"))));
    }

    private Long categoria(String slug, String nombre) {
        Long comercioId = comercios.findBySlug(slug).orElseThrow().getId();
        return TenantContext.ejecutarComo(comercioId, () -> categorias.findAll().stream()
                .filter(c -> c.getNombre().equals(nombre)).findFirst().orElseThrow().getId());
    }
}
