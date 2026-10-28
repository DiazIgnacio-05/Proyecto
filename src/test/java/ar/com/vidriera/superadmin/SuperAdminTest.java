package ar.com.vidriera.superadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ar.com.vidriera.PruebaDeIntegracion;
import ar.com.vidriera.catalogo.Categoria;
import ar.com.vidriera.catalogo.CategoriaRepository;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.EstadoComercio;
import ar.com.vidriera.comercio.Modulo;
import ar.com.vidriera.comercio.Slug;
import ar.com.vidriera.tenant.TenantContext;
import ar.com.vidriera.usuario.Usuario;
import ar.com.vidriera.usuario.UsuarioDetailsService;
import ar.com.vidriera.usuario.UsuarioRepository;

/** Panel de super-admin de punta a punta: alta, estados, abono, módulos y claves. */
@AutoConfigureMockMvc
class SuperAdminTest extends PruebaDeIntegracion {

    private static final String PANEL = "app.localhost";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ComercioRepository comercios;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private CategoriaRepository categorias;
    @Autowired
    private UsuarioDetailsService usuarioDetailsService;

    private final List<Long> creados = new ArrayList<>();

    @AfterEach
    void borrarCreados() {
        comercios.deleteAllById(creados);
        creados.clear();
    }

    @Test
    void unDuenoNoPuedeEntrarAlPanelDeSuperAdmin() throws Exception {
        mvc.perform(get("/superadmin").header("Host", PANEL)
                        .with(user(usuarioDetailsService.loadUserByUsername("sabores@vidriera.test"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/superadmin").header("Host", PANEL)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void elListadoMuestraTodosLosComerciosConProductosYAbono() throws Exception {
        mvc.perform(get("/superadmin").header("Host", PANEL).with(superAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sabores del Valle")))
                .andExpect(content().string(containsString("Nogal Indumentaria")))
                // Productos contados en todos los comercios (consulta en modo sistema).
                .andExpect(content().string(containsString("8 productos")))
                .andExpect(content().string(containsString("Vencido hace 3 días")));

        mvc.perform(get("/superadmin").param("filtro", "VENCIDOS").header("Host", PANEL).with(superAdmin()))
                .andExpect(content().string(containsString("Bazar La Alameda")))
                .andExpect(content().string(not(containsString("Nogal Indumentaria"))));
    }

    @Test
    void daDeAltaUnComercioConDuenoContrasenaTemporalYCategoriasDelRubro() throws Exception {
        String nombre = "Kiosco " + UUID.randomUUID().toString().substring(0, 6);
        String email = "dueno-" + UUID.randomUUID().toString().substring(0, 6) + "@prueba.test";
        MvcResult resultado = mvc.perform(alta(nombre, "", "ARTICULOS", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("ok", "Comercio creado. Pasale estos datos al dueño."))
                .andReturn();

        Comercio comercio = comercios.findBySlug(Slug.sugerir(nombre)).orElseThrow();
        creados.add(comercio.getId());
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/superadmin/comercios/" + comercio.getId());
        assertThat(comercio.getEstado()).isEqualTo(EstadoComercio.ACTIVO);
        assertThat(TenantContext.ejecutarComo(comercio.getId(), () -> categorias.findAll()))
                .extracting(Categoria::getNombre).containsExactly("Destacados", "Hogar", "Regalería");

        Usuario dueno = usuarios.findByEmail(email).orElseThrow();
        assertThat(dueno.isDebeCambiarPassword()).isTrue();

        // Con la contraseña temporal entra, y el panel lo manda primero a cambiarla.
        SuperAdminController.Credenciales credenciales =
                (SuperAdminController.Credenciales) resultado.getFlashMap().get("credenciales");
        assertThat(credenciales.contrasena()).matches("[a-z2-9]{4}-[a-z2-9]{4}-[a-z2-9]{4}");
        assertThat(credenciales.linkWhatsApp()).startsWith("https://wa.me/5493834555555?text=");
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/login").header("Host", PANEL).with(csrf()).session(sesion)
                        .param("email", email).param("password", credenciales.contrasena()))
                .andExpect(redirectedUrl("/admin"));
        mvc.perform(get("/admin").header("Host", PANEL).session(sesion)).andExpect(redirectedUrl("/admin/cuenta"));

        // Y el sitio ya existe en su subdominio.
        mvc.perform(get("/").header("Host", comercio.getSlug() + ".localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(nombre)));
    }

    @Test
    void rechazaSubdominiosYEmailsRepetidosOWhatsAppInvalido() throws Exception {
        mvc.perform(alta("Otro Nogal", "nogal", "ROPA", "nuevo-" + UUID.randomUUID() + "@prueba.test"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya hay un comercio con el subdominio")));
        mvc.perform(alta("Comercio Nuevo", "comercio-" + UUID.randomUUID().toString().substring(0, 6), "ROPA",
                        "sabores@vidriera.test"))
                .andExpect(content().string(containsString("Ya existe un usuario con el email")));
        mvc.perform(alta("Comercio Nuevo", "www", "ROPA", "nuevo-" + UUID.randomUUID() + "@prueba.test"))
                .andExpect(content().string(containsString("está reservado")));
        mvc.perform(post("/superadmin/comercios").header("Host", PANEL).with(superAdmin()).with(csrf())
                        .param("nombre", "Comercio Nuevo").param("rubro", "ROPA").param("whatsapp", "123")
                        .param("nombreDueno", "Ana").param("emailDueno", "nuevo-" + UUID.randomUUID() + "@prueba.test")
                        .param("vencimientoAbono", LocalDate.now().plusMonths(1).toString()))
                .andExpect(content().string(containsString("No reconocemos el WhatsApp")));
    }

    @Test
    void suspenderPagarYDarDeBaja() throws Exception {
        Long id = crearPorAlta("ROPA");
        Comercio comercio = comercios.findById(id).orElseThrow();
        String sitio = comercio.getSlug() + ".localhost";

        mvc.perform(post("/superadmin/comercios/{id}/suspender", id).header("Host", PANEL).with(superAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/superadmin/comercios/" + id));
        mvc.perform(get("/").header("Host", sitio)).andExpect(status().isServiceUnavailable());

        // Registrar el pago suma un mes y reactiva.
        LocalDate vencimientoAnterior = comercios.findById(id).orElseThrow().getVencimientoAbono();
        mvc.perform(post("/superadmin/comercios/{id}/pago", id).header("Host", PANEL).with(superAdmin()).with(csrf()))
                .andExpect(flash().attribute("ok", containsString("Pago registrado")));
        Comercio pagado = comercios.findById(id).orElseThrow();
        assertThat(pagado.getEstado()).isEqualTo(EstadoComercio.ACTIVO);
        assertThat(pagado.getVencimientoAbono()).isEqualTo(vencimientoAnterior.plusMonths(1));
        mvc.perform(get("/").header("Host", sitio)).andExpect(status().isOk());

        mvc.perform(post("/superadmin/comercios/{id}/baja", id).header("Host", PANEL).with(superAdmin()).with(csrf()));
        mvc.perform(get("/").header("Host", sitio)).andExpect(status().isNotFound());
        assertThat(comercios.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoComercio.BAJA);
    }

    @Test
    void resetearLaContrasenaDelDueno() throws Exception {
        Long id = crearPorAlta("RESTAURANTE");
        Usuario dueno = usuarios.findByComercioIdOrderByIdAsc(id).getFirst();
        dueno.cambiarPassword("x"); // simula que ya la había cambiado
        usuarios.save(dueno);

        MvcResult resultado = mvc.perform(post("/superadmin/comercios/{id}/usuarios/{u}/contrasena", id, dueno.getId())
                        .header("Host", PANEL).with(superAdmin()).with(csrf()))
                .andExpect(flash().attribute("ok", "Se generó una contraseña temporal nueva."))
                .andReturn();
        String temporal = ((SuperAdminController.Credenciales) resultado.getFlashMap().get("credenciales")).contrasena();
        assertThat(usuarios.findById(dueno.getId()).orElseThrow().isDebeCambiarPassword()).isTrue();
        mvc.perform(post("/login").header("Host", PANEL).with(csrf())
                        .param("email", dueno.getEmail()).param("password", temporal))
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void noReseteaUsuariosDeOtroComercio() throws Exception {
        Long id = crearPorAlta("RESTAURANTE");
        Long duenoDeSabores = usuarios.findByEmail("sabores@vidriera.test").orElseThrow().getId();
        mvc.perform(post("/superadmin/comercios/{id}/usuarios/{u}/contrasena", id, duenoDeSabores)
                        .header("Host", PANEL).header("Referer", "http://app.localhost/superadmin/comercios/" + id)
                        .with(superAdmin()).with(csrf()))
                .andExpect(flash().attribute("error", "Ese usuario no pertenece al comercio"));
    }

    @Test
    void conElModuloDeDominioPropioElSitioRespondeEnEseDominio() throws Exception {
        Long id = crearPorAlta("RESTAURANTE");
        String dominio = "www.prueba-" + UUID.randomUUID().toString().substring(0, 6) + ".com.ar";
        mvc.perform(post("/superadmin/comercios/{id}/modulos", id).header("Host", PANEL).with(superAdmin()).with(csrf())
                        .param("modulo", Modulo.DOMINIO_PROPIO.name()).param("dominioPropio", dominio))
                .andExpect(flash().attribute("ok", "Módulos guardados"));

        Comercio comercio = comercios.findById(id).orElseThrow();
        assertThat(comercio.tieneModulo(Modulo.DOMINIO_PROPIO)).isTrue();
        mvc.perform(get("/").header("Host", dominio))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(comercio.getNombre())));
    }

    @Test
    void laFichaDelComercioCarga() throws Exception {
        Long id = crearPorAlta("ROPA");
        mvc.perform(get("/superadmin/comercios/{id}", id).header("Host", PANEL).with(superAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registrar pago de un mes")));
        mvc.perform(get("/superadmin/comercios/nuevo").header("Host", PANEL).with(superAdmin()))
                .andExpect(status().isOk());
        mvc.perform(get("/superadmin/cuenta").header("Host", PANEL).with(superAdmin()))
                .andExpect(status().isOk());
        mvc.perform(get("/superadmin/comercios/999999").header("Host", PANEL).with(superAdmin()))
                .andExpect(status().isNotFound());
    }

    private Long crearPorAlta(String rubro) throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(alta("Comercio " + sufijo, "prueba-" + sufijo, rubro, sufijo + "@prueba.test"))
                .andExpect(status().is3xxRedirection());
        Long id = comercios.findBySlug("prueba-" + sufijo).orElseThrow().getId();
        creados.add(id);
        return id;
    }

    private MockHttpServletRequestBuilder alta(String nombre, String slug,
            String rubro, String email) {
        return post("/superadmin/comercios").header("Host", PANEL).with(superAdmin()).with(csrf())
                .param("nombre", nombre).param("slug", slug).param("rubro", rubro)
                .param("whatsapp", "383 4555555").param("nombreDueno", "Dueño de prueba").param("emailDueno", email)
                .param("vencimientoAbono", LocalDate.now().plusMonths(1).toString());
    }

    private RequestPostProcessor superAdmin() {
        return user(usuarioDetailsService.loadUserByUsername("superadmin@vidriera.test"));
    }
}
