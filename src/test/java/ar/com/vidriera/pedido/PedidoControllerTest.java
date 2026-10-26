package ar.com.vidriera.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ar.com.vidriera.PruebaDeIntegracion;
import ar.com.vidriera.catalogo.Producto;
import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.tenant.TenantContext;

/** El pedido de punta a punta: formulario → validación con la base → redirección a WhatsApp. */
@AutoConfigureMockMvc
class PedidoControllerTest extends PruebaDeIntegracion {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ComercioRepository comercios;
    @Autowired
    private ProductoRepository productos;

    private Map<String, Long> sabores;
    private Map<String, Long> nogal;

    @BeforeEach
    void cargarIdsDeProductos() {
        sabores = idsPorNombre("sabores-del-valle");
        nogal = idsPorNombre("nogal");
    }

    @Test
    void redirigeAWhatsAppConElMensajeArmadoConLosPreciosDeLaBase() throws Exception {
        String destino = mvc.perform(pedidoSabores()
                        .param("lineas[0].productoId", id("Empanada de carne cortada a cuchillo"))
                        .param("lineas[0].cantidad", "6")
                        .param("lineas[1].productoId", id("Locro criollo"))
                        .param("lineas[1].cantidad", "2")
                        .param("modalidad", "MESA")
                        .param("mesa", "4")
                        .param("comentario", "Una sin sal"))
                .andExpect(status().isSeeOther())
                .andReturn().getResponse().getRedirectedUrl();

        assertThat(destino).startsWith("https://wa.me/5493834000001?text=");
        assertThat(mensaje(destino))
                .contains("• 6 x Empanada de carne cortada a cuchillo — $ 10.800")
                .contains("• 2 x Locro criollo — $ 24.000")
                .contains("*Total: $ 34.800*")
                .contains("*Mesa 4*")
                .contains("Comentario: Una sin sal");
    }

    @Test
    void conEnvioIncluyeLaDireccion() throws Exception {
        String destino = mvc.perform(pedidoSabores()
                        .param("lineas[0].productoId", id("Locro criollo"))
                        .param("lineas[0].cantidad", "1")
                        .param("modalidad", "ENVIO")
                        .param("nombre", "Ana")
                        .param("direccion", "Esquiú 300"))
                .andExpect(status().isSeeOther())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(mensaje(destino)).contains("*Envío a domicilio:* Esquiú 300").contains("Nombre: Ana");
    }

    @Test
    void rechazaProductosSinStock() throws Exception {
        mvc.perform(pedidoSabores()
                        .param("lineas[0].productoId", id("Cabrito al horno"))
                        .param("lineas[0].cantidad", "1")
                        .param("modalidad", "RETIRO")
                        .param("nombre", "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Cabrito al horno ya no está disponible")));
    }

    @Test
    void noSePuedePedirUnProductoDeOtroComercio() throws Exception {
        mvc.perform(pedidoSabores()
                        .param("lineas[0].productoId", String.valueOf(nogal.get("Gorra trucker")))
                        .param("lineas[0].cantidad", "1")
                        .param("modalidad", "RETIRO")
                        .param("nombre", "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("ya no está disponible")));
    }

    @Test
    void rechazaMesasInexistentesYEnviosEnComerciosQueNoLosHacen() throws Exception {
        mvc.perform(pedidoSabores()
                        .param("lineas[0].productoId", id("Locro criollo"))
                        .param("lineas[0].cantidad", "1")
                        .param("modalidad", "MESA")
                        .param("mesa", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("La mesa indicada no existe")));

        mvc.perform(post("/pedido").header("Host", "la-alameda.localhost")
                        .param("lineas[0].productoId", String.valueOf(idsPorNombre("la-alameda").get("Termo de acero 1 L")))
                        .param("lineas[0].cantidad", "1")
                        .param("modalidad", "ENVIO")
                        .param("nombre", "Ana")
                        .param("direccion", "Esquiú 300"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("no hace envíos")));
    }

    @Test
    void validaElFormulario() throws Exception {
        mvc.perform(pedidoSabores().param("modalidad", "RETIRO").param("nombre", "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("El pedido está vacío")));

        mvc.perform(pedidoSabores()
                        .param("lineas[0].productoId", id("Locro criollo"))
                        .param("lineas[0].cantidad", "500")
                        .param("modalidad", "RETIRO")
                        .param("nombre", "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("La cantidad máxima es 99")));
    }

    @Test
    void enElDominioDeLaPlataformaNoHayPedidos() throws Exception {
        mvc.perform(post("/pedido").header("Host", "localhost")
                        .param("lineas[0].productoId", "1").param("lineas[0].cantidad", "1")
                        .param("modalidad", "RETIRO").param("nombre", "Ana"))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder pedidoSabores() {
        return post("/pedido").header("Host", "sabores-del-valle.localhost");
    }

    private String id(String nombre) {
        return String.valueOf(sabores.get(nombre));
    }

    private Map<String, Long> idsPorNombre(String slug) {
        Long comercioId = comercios.findBySlug(slug).orElseThrow().getId();
        return TenantContext.ejecutarComo(comercioId, () -> productos.findAll().stream()
                .collect(Collectors.toMap(Producto::getNombre, Producto::getId)));
    }

    private static String mensaje(String urlWhatsApp) {
        return URLDecoder.decode(urlWhatsApp.substring(urlWhatsApp.indexOf("?text=") + 6), StandardCharsets.UTF_8);
    }
}
