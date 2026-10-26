package ar.com.vidriera;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.com.vidriera.catalogo.Producto;
import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.comercio.CalendarioAtencion;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.HorarioAtencion;
import ar.com.vidriera.comercio.HorarioAtencionRepository;
import ar.com.vidriera.comercio.Rubro;
import ar.com.vidriera.tenant.TenantContext;
import ar.com.vidriera.usuario.Rol;
import ar.com.vidriera.usuario.UsuarioAutenticado;
import ar.com.vidriera.usuario.UsuarioDetailsService;
import ar.com.vidriera.usuario.UsuarioRepository;

/**
 * Arranca la aplicación con las migraciones reales: si una entidad no coincide con su tabla,
 * Hibernate ({@code ddl-auto: validate}) hace fallar este test. Además revisa los datos demo.
 */
class DatosDemoTest extends PruebaDeIntegracion {

    @Autowired
    private ComercioRepository comercios;
    @Autowired
    private ProductoRepository productos;
    @Autowired
    private HorarioAtencionRepository horarios;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private UsuarioDetailsService usuarioDetailsService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void existenLosTresComerciosDemoUnoPorRubro() {
        assertThat(List.of("sabores-del-valle", "nogal", "la-alameda"))
                .map(slug -> comercios.findBySlug(slug).orElseThrow().getRubro())
                .containsExactly(Rubro.RESTAURANTE, Rubro.ROPA, Rubro.ARTICULOS);
    }

    @Test
    void cadaComercioDemoTieneProductosYHorarios() {
        for (String slug : List.of("sabores-del-valle", "nogal", "la-alameda")) {
            Long id = comercios.findBySlug(slug).orElseThrow().getId();
            assertThat(TenantContext.ejecutarComo(id, () -> productos.count())).as(slug).isPositive();
            assertThat(TenantContext.ejecutarComo(id, () -> horarios.count())).as(slug).isPositive();
        }
    }

    @Test
    void laTiendaDeRopaTieneVariantesConTallesAgotados() {
        Long nogal = comercios.findBySlug("nogal").orElseThrow().getId();
        List<Producto> catalogo = TenantContext.ejecutarComo(nogal, () -> productos.findAllConVariantes());
        assertThat(catalogo).anyMatch(p -> p.getVariantes().stream().anyMatch(v -> !v.isDisponible()));
        // La campera de jean tiene todas sus variantes agotadas: no se puede pedir.
        assertThat(catalogo).filteredOn(p -> p.getNombre().equals("Campera de jean"))
                .singleElement()
                .matches(p -> !p.sePuedePedir());
    }

    @Test
    void losHorariosDelRestauranteCruzanLaMedianoche() {
        Long id = comercios.findBySlug("sabores-del-valle").orElseThrow().getId();
        CalendarioAtencion calendario = new CalendarioAtencion(TenantContext.ejecutarComo(id,
                () -> horarios.findAll().stream().map(HorarioAtencion::getFranja).toList()));
        // Miércoles 28/10/2026 a las 00:15: sigue abierto el turno noche del martes.
        assertThat(calendario.estaAbierto(java.time.LocalDateTime.of(2026, 10, 28, 0, 15))).isTrue();
    }

    @Test
    void elDuenoDemoPuedeAutenticarseConSuComercio() {
        UsuarioAutenticado dueno = usuarioDetailsService.loadUserByUsername("Sabores@Vidriera.test");
        Comercio sabores = comercios.findBySlug("sabores-del-valle").orElseThrow();
        assertThat(dueno.getRol()).isEqualTo(Rol.DUENO);
        assertThat(dueno.getComercioId()).isEqualTo(sabores.getId());
        assertThat(passwordEncoder.matches("demo1234", dueno.getPassword())).isTrue();
    }

    @Test
    void seCreoElSuperAdminDesdeLaConfiguracion() {
        assertThat(usuarios.findByEmail("superadmin@vidriera.test"))
                .hasValueSatisfying(u -> {
                    assertThat(u.getRol()).isEqualTo(Rol.SUPER_ADMIN);
                    assertThat(passwordEncoder.matches("clave-de-test-123", u.getPasswordHash())).isTrue();
                });
    }
}
