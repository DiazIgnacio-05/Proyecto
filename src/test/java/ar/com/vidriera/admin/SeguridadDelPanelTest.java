package ar.com.vidriera.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.usuario.Usuario;

/** Login, acceso al panel y reglas por estado del comercio. */
class SeguridadDelPanelTest extends PruebaDelPanel {

    @Test
    void sinLoginElPanelRedirigeAlLogin() throws Exception {
        mvc.perform(get("/admin/productos").header("Host", HOST_PANEL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void conEmailYContrasenaCorrectosEntraAlPanel() throws Exception {
        ComercioDePrueba c = crearComercio("login");
        mvc.perform(post("/login").header("Host", HOST_PANEL).with(csrf())
                        .param("email", c.email()).param("password", PASSWORD))
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void conContrasenaIncorrectaVuelveAlLoginConError() throws Exception {
        ComercioDePrueba c = crearComercio("login");
        mvc.perform(post("/login").header("Host", HOST_PANEL).with(csrf())
                        .param("email", c.email()).param("password", "incorrecta"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void despuesDeCincoIntentosFallidosSeBloqueaAunqueAcierte() throws Exception {
        ComercioDePrueba c = crearComercio("bloqueo");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").header("Host", HOST_PANEL).with(csrf())
                    .param("email", c.email()).param("password", "incorrecta-" + i));
        }
        mvc.perform(post("/login").header("Host", HOST_PANEL).with(csrf())
                        .param("email", c.email()).param("password", PASSWORD))
                .andExpect(redirectedUrl("/login?bloqueado"));
    }

    @Test
    void elLoginExigeTokenCsrf() throws Exception {
        ComercioDePrueba c = crearComercio("csrf");
        mvc.perform(post("/login").header("Host", HOST_PANEL)
                        .param("email", c.email()).param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasAccionesDelPanelExigenTokenCsrf() throws Exception {
        ComercioDePrueba c = crearComercio("csrf");
        mvc.perform(post("/admin/productos/{id}/disponible", c.productoId())
                        .header("Host", HOST_PANEL).with(comoDueno(c)))
                .andExpect(status().isForbidden());
        assertThat(producto(c, c.productoId()).isDisponible()).isTrue();
    }

    @Test
    void elPanelYElLoginNoExistenEnElSubdominioDeUnComercio() throws Exception {
        ComercioDePrueba c = crearComercio("host");
        String hostComercio = c.slug() + ".localhost";
        mvc.perform(get("/login").header("Host", hostComercio)).andExpect(status().isNotFound());
        mvc.perform(get("/admin").header("Host", hostComercio).with(comoDueno(c)))
                .andExpect(status().isNotFound());
    }

    @Test
    void elSuperAdminNoEntraAlPanelDeComercios() throws Exception {
        mvc.perform(get("/admin").header("Host", HOST_PANEL)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(usuarioDetailsService.loadUserByUsername("superadmin@vidriera.test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unComercioSuspendidoPuedeVerPeroNoModificar() throws Exception {
        ComercioDePrueba c = crearComercio("suspendido");
        Comercio comercio = comercios.findById(c.id()).orElseThrow();
        comercio.suspender();
        comercios.save(comercio);

        mvc.perform(get("/admin/productos").header("Host", HOST_PANEL).with(comoDueno(c)))
                .andExpect(status().isOk());
        mvc.perform(post("/admin/productos/{id}/disponible", c.productoId())
                        .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()))
                .andExpect(redirectedUrl("/admin?soloLectura"));
        assertThat(producto(c, c.productoId()).isDisponible()).isTrue();
    }

    @Test
    void unComercioDadoDeBajaNoPuedeEntrar() throws Exception {
        ComercioDePrueba c = crearComercio("baja");
        Comercio comercio = comercios.findById(c.id()).orElseThrow();
        comercio.darDeBaja();
        comercios.save(comercio);

        mvc.perform(post("/login").header("Host", HOST_PANEL).with(csrf())
                        .param("email", c.email()).param("password", PASSWORD))
                .andExpect(redirectedUrl("/login?deshabilitado"));
    }

    @Test
    void conContrasenaTemporalSoloPuedeIrAMiCuentaHastaCambiarla() throws Exception {
        ComercioDePrueba c = crearComercio("temporal");
        Usuario usuario = usuarios.findByEmail(c.email()).orElseThrow();
        usuario.exigirCambioDePassword();
        usuarios.save(usuario);

        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/login").header("Host", HOST_PANEL).with(csrf()).session(sesion)
                .param("email", c.email()).param("password", PASSWORD));
        mvc.perform(get("/admin/productos").header("Host", HOST_PANEL).session(sesion))
                .andExpect(redirectedUrl("/admin/cuenta"));

        mvc.perform(post("/admin/cuenta/password").header("Host", HOST_PANEL).session(sesion).with(csrf())
                        .param("actual", PASSWORD)
                        .param("nueva", "una frase nueva y larga")
                        .param("repetida", "una frase nueva y larga"))
                .andExpect(redirectedUrl("/admin"));

        // Con la misma sesión ya puede usar el panel, y la contraseña nueva quedó guardada.
        mvc.perform(get("/admin/productos").header("Host", HOST_PANEL).session(sesion))
                .andExpect(status().isOk());
        Usuario actualizado = usuarios.findByEmail(c.email()).orElseThrow();
        assertThat(actualizado.isDebeCambiarPassword()).isFalse();
        assertThat(passwordEncoder.matches("una frase nueva y larga", actualizado.getPasswordHash())).isTrue();
    }

    @Test
    void elCambioDeContrasenaValidaLaActualYElLargo() throws Exception {
        ComercioDePrueba c = crearComercio("password");
        mvc.perform(post("/admin/cuenta/password").header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf())
                        .param("actual", "incorrecta").param("nueva", "otra clave larga").param("repetida", "otra clave larga"))
                .andExpect(redirectedUrl("/admin/cuenta"));
        mvc.perform(post("/admin/cuenta/password").header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf())
                        .param("actual", PASSWORD).param("nueva", "corta").param("repetida", "corta"))
                .andExpect(redirectedUrl("/admin/cuenta"));
        assertThat(passwordEncoder.matches(PASSWORD, usuarios.findByEmail(c.email()).orElseThrow().getPasswordHash()))
                .isTrue();
    }
}
