package ar.com.vidriera.tenant;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;

/**
 * Para los requests del sitio público: identifica el comercio por el host, lo deja disponible como
 * atributo del request y lo establece en {@link TenantContext} mientras dura el request.
 */
@Component
public class FiltroComercioPorHost extends OncePerRequestFilter {

    public static final String ATRIBUTO_COMERCIO = "vidriera.comercio";

    private final ResolutorDeHost resolutor;
    private final ComercioRepository comercios;

    public FiltroComercioPorHost(ResolutorDeHost resolutor, ComercioRepository comercios) {
        this.resolutor = resolutor;
        this.comercios = comercios;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/img/")
                || ruta.startsWith("/imagenes/")
                || ruta.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain cadena) throws ServletException, IOException {

        DestinoHost destino = resolutor.resolver(request.getServerName());
        if (destino instanceof DestinoHost.Plataforma) {
            cadena.doFilter(request, response);
            return;
        }

        // Los paneles y el login viven solo en el host de la plataforma (app.<dominio>): en el
        // subdominio de un comercio no existen. Así el comercio del panel sale siempre del usuario.
        if (esRutaDePanel(request.getRequestURI())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Optional<Comercio> comercio = switch (destino) {
            case DestinoHost.Plataforma p -> Optional.empty();
            case DestinoHost.SitioDeComercio sitio -> comercios.findBySlug(sitio.slug());
            case DestinoHost.DominioPropio dominio -> comercios.findByDominioPropio(dominio.host());
        };
        if (comercio.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute(ATRIBUTO_COMERCIO, comercio.get());
        TenantContext.establecer(comercio.get().getId());
        try {
            cadena.doFilter(request, response);
        } finally {
            TenantContext.limpiar();
        }
    }

    static boolean esRutaDePanel(String ruta) {
        return ruta.equals("/admin") || ruta.startsWith("/admin/")
                || ruta.equals("/superadmin") || ruta.startsWith("/superadmin/")
                || ruta.equals("/login") || ruta.equals("/logout");
    }
}
