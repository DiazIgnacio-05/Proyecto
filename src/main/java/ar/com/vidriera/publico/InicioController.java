package ar.com.vidriera.publico;

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
import ar.com.vidriera.comercio.Rubro;
import ar.com.vidriera.pedido.NumeroWhatsApp;
import ar.com.vidriera.tenant.FiltroComercioPorHost;

/**
 * Página de inicio: en el dominio de la plataforma, la landing; en el subdominio de un comercio,
 * su sitio público (por ahora el menú del restaurante; los otros rubros llegan en la Etapa 4).
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
        if (comercio.getRubro() != Rubro.RESTAURANTE) {
            model.addAttribute("cantidadProductos",
                    catalogo.secciones().stream().mapToLong(s -> s.productos().size()).sum());
            return "publico/en-construccion";
        }

        String urlBase = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        model.addAttribute("meta", MetadatosPagina.de(comercio, urlBase));
        model.addAttribute("secciones", catalogo.secciones());
        model.addAttribute("atencion", atencion.infoActual());
        model.addAttribute("mesa", mesaValida(comercio, mesa).orElse(null));
        model.addAttribute("whatsapp", whatsapp(comercio).orElse(null));
        return "publico/restaurante";
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
