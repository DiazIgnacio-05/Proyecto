package ar.com.vidriera.admin;

import java.time.Duration;
import java.util.stream.IntStream;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.UrlsDelComercio;
import ar.com.vidriera.qr.GeneradorQr;

/**
 * QR de las mesas, dentro del panel. Cada QR apunta al sitio público del comercio con
 * {@code ?mesa=N}; como el panel vive en otro host, la URL se arma con {@link UrlsDelComercio}.
 */
@Controller
@RequestMapping("/admin/mesas")
class MesasAdminController {

    private static final MediaType SVG = MediaType.valueOf("image/svg+xml");

    private final GeneradorQr generador;
    private final UrlsDelComercio urls;

    MesasAdminController(GeneradorQr generador, UrlsDelComercio urls) {
        this.generador = generador;
        this.urls = urls;
    }

    @ModelAttribute("seccion")
    String seccion() {
        return "mesas";
    }

    @GetMapping
    String hoja(@RequestAttribute(FiltroPanelDelComercio.ATRIBUTO_COMERCIO) Comercio comercio, Model model) {
        if (!comercio.getRubro().usaMesas()) {
            throw new RecursoNoEncontradoException("Página");
        }
        model.addAttribute("mesas", IntStream.rangeClosed(1, comercio.getCantidadMesas()).boxed().toList());
        return "admin/mesas";
    }

    @GetMapping("/{numero}/qr.svg")
    ResponseEntity<String> qr(@RequestAttribute(FiltroPanelDelComercio.ATRIBUTO_COMERCIO) Comercio comercio,
            @PathVariable int numero, HttpServletRequest request) {
        if (!comercio.tieneMesa(numero)) {
            throw new RecursoNoEncontradoException("Mesa");
        }
        return ResponseEntity.ok()
                .contentType(SVG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(generador.svg(urls.mesa(comercio, numero, request)));
    }
}
