package ar.com.vidriera.publico;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.server.ResponseStatusException;

import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.tenant.FiltroComercioPorHost;

/**
 * Página de inicio. En un subdominio de comercio muestra (por ahora) una página provisoria que
 * confirma que el comercio se resolvió bien; en la Etapa 2 se reemplaza por el sitio real.
 */
@Controller
public class InicioController {

    private final ProductoRepository productos;

    public InicioController(ProductoRepository productos) {
        this.productos = productos;
    }

    @GetMapping("/")
    public String inicio(
            @RequestAttribute(name = FiltroComercioPorHost.ATRIBUTO_COMERCIO, required = false) Comercio comercio,
            Model model, HttpServletResponse response) {

        if (comercio == null) {
            return "plataforma/inicio";
        }
        model.addAttribute("comercio", comercio);
        return switch (comercio.getEstado()) {
            case BAJA -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            case SUSPENDIDO -> {
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                yield "publico/no-disponible";
            }
            case ACTIVO -> {
                model.addAttribute("cantidadProductos", productos.count());
                yield "publico/en-construccion";
            }
        };
    }
}
