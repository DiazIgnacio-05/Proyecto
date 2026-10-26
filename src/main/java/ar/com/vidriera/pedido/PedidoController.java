package ar.com.vidriera.pedido;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.tenant.FiltroComercioPorHost;

/**
 * Recibe el pedido y redirige a WhatsApp con una respuesta 303 ("See Other": el navegador hace un
 * GET a la nueva dirección). En el celular eso abre la app de WhatsApp con el mensaje escrito.
 */
@Controller
public class PedidoController {

    private final PedidoService pedidos;

    public PedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @PostMapping("/pedido")
    public ModelAndView enviar(
            @RequestAttribute(name = FiltroComercioPorHost.ATRIBUTO_COMERCIO, required = false) Comercio comercio,
            @Valid @ModelAttribute PedidoForm form, BindingResult errores) {

        if (comercio == null || !comercio.getEstado().sitioPublicoVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (errores.hasErrors()) {
            return error(comercio, errores.getAllErrors().getFirst().getDefaultMessage());
        }
        try {
            RedirectView aWhatsApp = new RedirectView(pedidos.linkDeWhatsApp(comercio, form));
            aWhatsApp.setStatusCode(HttpStatus.SEE_OTHER);
            aWhatsApp.setExposeModelAttributes(false);
            return new ModelAndView(aWhatsApp);
        } catch (PedidoInvalidoException e) {
            return error(comercio, e.getMessage());
        }
    }

    private ModelAndView error(Comercio comercio, String mensaje) {
        ModelAndView vista = new ModelAndView("pedido/error", HttpStatus.BAD_REQUEST);
        vista.addObject("comercio", comercio);
        vista.addObject("mensaje", mensaje);
        return vista;
    }
}
