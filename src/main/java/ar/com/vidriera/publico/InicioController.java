package ar.com.vidriera.publico;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.com.vidriera.catalogo.CatalogoService;
import ar.com.vidriera.comercio.AtencionService;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.pedido.NumeroWhatsApp;
import ar.com.vidriera.tenant.FiltroComercioPorHost;

/**
 * Página de inicio: en el dominio de la plataforma, la landing; en el subdominio de un comercio,
 * su sitio público: el menú (restaurante) o el catálogo con buscador (ropa y artículos).
 */
@Controller
public class InicioController {

    private final CatalogoService catalogo;
    private final AtencionService atencion;

    public InicioController(CatalogoService catalogo, AtencionService atencion) {
        this.catalogo = catalogo;
        this.atencion = atencion;
    }

    @GetMapping("/")
    public String inicio(
            @RequestAttribute(name = FiltroComercioPorHost.ATRIBUTO_COMERCIO, required = false) Comercio comercio,
            @RequestParam(name = "mesa", required = false) String mesa,
            @RequestParam(name = "q", required = false) String busqueda,
            @RequestParam(name = "categoria", required = false) String categoria,
            Model model, HttpServletResponse response) {

        if (comercio == null) {
            return "plataforma/inicio";
        }
        model.addAttribute("comercio", comercio);
        switch (comercio.getEstado()) {
            case BAJA -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            case SUSPENDIDO -> {
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                return "publico/no-disponible";
            }
            case ACTIVO -> {
            }
        }
        String urlBase = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        List<CatalogoService.Seccion> secciones = catalogo.secciones();
        model.addAttribute("meta", MetadatosPagina.de(comercio, urlBase));
        model.addAttribute("atencion", atencion.infoActual());
        model.addAttribute("mesa", mesaValida(comercio, mesa).orElse(null));
        model.addAttribute("whatsapp", whatsapp(comercio).orElse(null));

        // El restaurante muestra el menú completo; los rubros con buscador, la grilla filtrable.
        if (!comercio.getRubro().usaBuscador()) {
            model.addAttribute("secciones", secciones);
            return "publico/restaurante";
        }
        String texto = textoBuscado(busqueda);
        Long categoriaId = categoriaValida(secciones, categoria);
        model.addAttribute("todas", secciones);
        model.addAttribute("q", texto);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("busqueda", CatalogoService.filtrar(secciones, texto, categoriaId));
        return "publico/catalogo";
    }

    /** null si no se buscó nada (o solo espacios). */
    static String textoBuscado(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return null;
        }
        String limpio = busqueda.strip();
        return limpio.length() > 80 ? limpio.substring(0, 80) : limpio;
    }

    /** La categoría del filtro tiene que ser una de las visibles de este comercio; si no, se ignora. */
    static Long categoriaValida(List<CatalogoService.Seccion> secciones, String categoria) {
        if (categoria == null || !categoria.matches("\\d{1,18}")) {
            return null;
        }
        Long id = Long.valueOf(categoria);
        return secciones.stream().anyMatch(s -> s.id().equals(id)) ? id : null;
    }

    /** El número de mesa viene del QR; si no es válido para este comercio se ignora. */
    static Optional<Integer> mesaValida(Comercio comercio, String mesa) {
        if (mesa == null || !mesa.matches("\\d{1,3}")) {
            return Optional.empty();
        }
        int numero = Integer.parseInt(mesa);
        return comercio.tieneMesa(numero) ? Optional.of(numero) : Optional.empty();
    }

    private static Optional<NumeroWhatsApp> whatsapp(Comercio comercio) {
        try {
            return Optional.of(NumeroWhatsApp.de(comercio.getContacto().getWhatsapp()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
