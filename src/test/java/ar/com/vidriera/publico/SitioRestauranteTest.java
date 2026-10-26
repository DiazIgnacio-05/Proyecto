package ar.com.vidriera.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.vidriera.PruebaDeIntegracion;

@AutoConfigureMockMvc
class SitioRestauranteTest extends PruebaDeIntegracion {

    private static final String SABORES = "sabores-del-valle.localhost";

    @Autowired
    private MockMvc mvc;

    @Test
    void muestraElMenuConCategoriasPreciosYProductosSinStock() throws Exception {
        mvc.perform(get("/").header("Host", SABORES))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Platos regionales")))
                .andExpect(content().string(containsString("Locro criollo")))
                .andExpect(content().string(containsString("$ 12.000")))
                .andExpect(content().string(containsString("Sin stock")))   // el cabrito no está disponible
                .andExpect(content().string(containsString("Horarios y contacto")))
                .andExpect(content().string(containsString("Envío a domicilio")));
    }

    @Test
    void incluyeLasEtiquetasDeSeoYLaVistaPreviaParaWhatsApp() throws Exception {
        mvc.perform(get("/").header("Host", SABORES))
                .andExpect(content().string(containsString(
                        "<meta property=\"og:title\" content=\"Sabores del Valle · Cocina catamarqueña\">")))
                .andExpect(content().string(containsString(
                        "<meta property=\"og:image\" content=\"http://sabores-del-valle.localhost/vista-previa.png\">")))
                .andExpect(content().string(containsString("--color-primario: #8C2F1B;")));
    }

    @Test
    void conElQrDeUnaMesaElPedidoVaALaMesa() throws Exception {
        mvc.perform(get("/").param("mesa", "5").header("Host", SABORES))
                .andExpect(content().string(containsString("mesa 5")))
                .andExpect(content().string(containsString("value=\"MESA\" checked")));
    }

    @Test
    void ignoraMesasQueNoExisten() throws Exception {
        for (String mesa : new String[] {"99", "0", "abc", "-1"}) {
            mvc.perform(get("/").param("mesa", mesa).header("Host", SABORES))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("value=\"MESA\""))));
        }
    }

    @Test
    void laVistaPreviaEsUnPng() throws Exception {
        byte[] png = mvc.perform(get("/vista-previa.png").header("Host", SABORES))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(png).startsWith(0x89, 'P', 'N', 'G');
    }

    @Test
    void cadaMesaTieneSuQrYLaHojaParaImprimir() throws Exception {
        mvc.perform(get("/mesas/12/qr.svg").header("Host", SABORES))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")))
                .andExpect(content().string(containsString("<svg")));
        mvc.perform(get("/mesas/13/qr.svg").header("Host", SABORES)).andExpect(status().isNotFound());
        mvc.perform(get("/mesas/imprimir").header("Host", SABORES))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mesa 12")));
    }

    @Test
    void losComerciosSinMesasNoTienenQr() throws Exception {
        mvc.perform(get("/mesas/1/qr.svg").header("Host", "nogal.localhost")).andExpect(status().isNotFound());
        mvc.perform(get("/mesas/imprimir").header("Host", "nogal.localhost")).andExpect(status().isNotFound());
    }

    @Test
    void elCssLlevaHashEnElNombreYSeCacheaUnAnio() throws Exception {
        String html = mvc.perform(get("/").header("Host", SABORES)).andReturn().getResponse().getContentAsString();
        String css = html.replaceAll("(?s).*href=\"(/css/sitio-[0-9a-f]+\\.css)\".*", "$1");
        assertThat(css).startsWith("/css/sitio-");
        mvc.perform(get(css).header("Host", SABORES))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")));
    }
}
