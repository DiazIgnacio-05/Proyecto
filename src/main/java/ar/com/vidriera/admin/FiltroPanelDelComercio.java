package ar.com.vidriera.admin;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.tenant.TenantContext;
import ar.com.vidriera.usuario.UsuarioAutenticado;

/**
 * Para las rutas {@code /admin/**}: toma el comercio del usuario logueado (nunca de la URL ni del
 * formulario), lo fija como tenant y aplica las reglas de estado:
 * <ul>
 *   <li>comercio dado de baja: se cierra la sesión;</li>
 *   <li>contraseña temporal: solo puede ir a "Mi cuenta" hasta cambiarla;</li>
 *   <li>comercio suspendido: puede mirar pero no guardar cambios.</li>
 * </ul>
 * Corre después de Spring Security, así que el usuario ya está autenticado.
 */
@Component
public class FiltroPanelDelComercio extends OncePerRequestFilter {

    public static final String ATRIBUTO_COMERCIO = "vidriera.comercioDelPanel";
    static final String RUTA_CUENTA = "/admin/cuenta";

    private final ComercioRepository comercios;

    public FiltroPanelDelComercio(ComercioRepository comercios) {
        this.comercios = comercios;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return !(ruta.equals("/admin") || ruta.startsWith("/admin/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain cadena) throws ServletException, IOException {

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario)
                || usuario.getComercioId() == null) {
            cadena.doFilter(request, response); // Spring Security ya rechazó o va a rechazar el acceso
            return;
        }

        Comercio comercio = comercios.findById(usuario.getComercioId()).orElse(null);
        if (comercio == null || !comercio.getEstado().permiteIngresoAlPanel()) {
            request.logout();
            response.sendRedirect("/login?deshabilitado");
            return;
        }

        String ruta = request.getRequestURI();
        boolean enCuenta = ruta.startsWith(RUTA_CUENTA);
        if (usuario.isDebeCambiarPassword() && !enCuenta) {
            response.sendRedirect(RUTA_CUENTA);
            return;
        }
        boolean modifica = !"GET".equalsIgnoreCase(request.getMethod());
        if (modifica && !enCuenta && !comercio.getEstado().permiteEditar()) {
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
            response.setHeader("Location", "/admin?soloLectura");
            return;
        }

        request.setAttribute(ATRIBUTO_COMERCIO, comercio);
        TenantContext.establecer(comercio.getId());
        try {
            cadena.doFilter(request, response);
        } finally {
            TenantContext.limpiar();
        }
    }
}
