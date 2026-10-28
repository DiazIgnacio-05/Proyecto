package ar.com.vidriera.tenant;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import ar.com.vidriera.PruebaDeIntegracion;

/** Prueba de punta a punta: el host del request decide qué comercio se muestra. */
@AutoConfigureMockMvc
class FiltroComercioPorHostTest extends PruebaDeIntegracion {

    @Autowired
    private MockMvc mvc;

    @Test
    void elSubdominioMuestraElComercioCorrespondiente() throws Exception {
        mvc.perform(get("/").header("Host", "nogal.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nogal Indumentaria")))
                .andExpect(content().string(containsString("Remera básica de algodón")));
    }

    @Test
    void unSubdominioInexistenteDa404() throws Exception {
        mvc.perform(get("/").header("Host", "no-existe.localhost"))
                .andExpect(status().isNotFound());
    }

    @Test
    void elDominioBaseMuestraLaPlataforma() throws Exception {
        mvc.perform(get("/").header("Host", "localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sitios web para comercios")));
    }

    @Test
    void losPanelesRequierenLogin() throws Exception {
        mvc.perform(get("/admin/").header("Host", "app.localhost"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/superadmin/").header("Host", "app.localhost"))
                .andExpect(status().is3xxRedirection());
    }
}
