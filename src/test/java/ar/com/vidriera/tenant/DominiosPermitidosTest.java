package ar.com.vidriera.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.vidriera.PruebaDeIntegracion;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.Modulo;
import ar.com.vidriera.comercio.Rubro;

/** Caddy solo pide certificados HTTPS para dominios propios dados de alta. */
@AutoConfigureMockMvc
class DominiosPermitidosTest extends PruebaDeIntegracion {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ComercioRepository comercios;

    private Comercio comercio;

    @AfterEach
    void borrar() {
        if (comercio != null) {
            comercios.delete(comercio);
        }
    }

    private String crearConDominio(boolean conModulo) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String dominio = "www.cert-" + sufijo + ".com.ar";
        Comercio nuevo = new Comercio("cert-" + sufijo, "Prueba certificados", Rubro.RESTAURANTE,
                "3834000000", LocalDate.now());
        nuevo.setDominioPropio(dominio);
        if (conModulo) {
            nuevo.activarModulo(Modulo.DOMINIO_PROPIO);
        }
        comercio = comercios.save(nuevo);
        return dominio;
    }

    @Test
    void permiteElDominioPropioDeUnComercioConElModulo() throws Exception {
        String dominio = crearConDominio(true);
        // Caddy consulta con el host interno de Docker, que no es de ningún comercio.
        mvc.perform(get(DominiosPermitidosController.RUTA).param("domain", dominio.toUpperCase())
                        .header("Host", "app:8080"))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaDominiosDesconocidosOSinElModulo() throws Exception {
        String sinModulo = crearConDominio(false);
        mvc.perform(get(DominiosPermitidosController.RUTA).param("domain", sinModulo).header("Host", "app:8080"))
                .andExpect(status().isNotFound());
        mvc.perform(get(DominiosPermitidosController.RUTA).param("domain", "www.cualquiera.com").header("Host", "app:8080"))
                .andExpect(status().isNotFound());
        mvc.perform(get(DominiosPermitidosController.RUTA).header("Host", "app:8080"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaElDominioDeUnComercioDadoDeBaja() throws Exception {
        String dominio = crearConDominio(true);
        comercio.darDeBaja();
        comercio = comercios.save(comercio);
        mvc.perform(get(DominiosPermitidosController.RUTA).param("domain", dominio).header("Host", "app:8080"))
                .andExpect(status().isNotFound());
    }
}
