package ar.com.vidriera.admin;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.UrlsDelComercio;
import ar.com.vidriera.imagen.ImagenInvalidaException;

/**
 * Se aplica a todos los controladores del panel: agrega al modelo lo que usa el menú (comercio y
 * link al sitio) y convierte los errores "esperables" en un mensaje para el dueño.
 */
@ControllerAdvice(basePackageClasses = PanelAdvice.class)
class PanelAdvice {

    private final UrlsDelComercio urls;

    PanelAdvice(UrlsDelComercio urls) {
        this.urls = urls;
    }

    @ModelAttribute("comercio")
    Comercio comercio(HttpServletRequest request) {
        return (Comercio) request.getAttribute(FiltroPanelDelComercio.ATRIBUTO_COMERCIO);
    }

    @ModelAttribute("urlSitio")
    String urlSitio(HttpServletRequest request) {
        Comercio comercio = comercio(request);
        return comercio == null ? null : urls.sitio(comercio, request);
    }

    @ExceptionHandler({OperacionRechazadaException.class, ImagenInvalidaException.class})
    String rechazada(RuntimeException e, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", e.getMessage());
        return "redirect:" + volverA(request);
    }

    /** Vuelve a la página desde la que se envió el formulario (solo rutas del panel). */
    static String volverA(HttpServletRequest request) {
        String anterior = request.getHeader("Referer");
        if (anterior != null) {
            int inicioRuta = anterior.indexOf("/admin");
            if (inicioRuta >= 0) {
                return anterior.substring(inicioRuta);
            }
        }
        return "/admin";
    }
}
