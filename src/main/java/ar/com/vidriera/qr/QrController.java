package ar.com.vidriera.qr;

import java.time.Duration;
import java.util.stream.IntStream;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.tenant.FiltroComercioPorHost;

/**
 * QR por mesa: cada código abre el menú con {@code ?mesa=N}, y el pedido de WhatsApp indica la mesa.
 * La hoja para imprimir pasa al panel del comercio (con login) en la Etapa 3; mientras tanto es
 * pública, pero no tiene nada privado (son links al menú) y no se indexa.
 */
@Controller
public class QrController {

    private static final MediaType SVG = MediaType.valueOf("image/svg+xml");

    private final GeneradorQr generador;

    public QrController(GeneradorQr generador) {
        this.generador = generador;
    }

    @GetMapping("/mesas/{numero}/qr.svg")
    public ResponseEntity<String> qrDeMesa(
            @RequestAttribute(name = FiltroComercioPorHost.ATRIBUTO_COMERCIO, required = false) Comercio comercio,
            @PathVariable int numero) {
        if (comercio == null || !comercio.tieneMesa(numero)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .contentType(SVG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(generador.svg(urlDeMesa(numero)));
    }

    @GetMapping("/mesas/imprimir")
    public String hojaParaImprimir(
            @RequestAttribute(name = FiltroComercioPorHost.ATRIBUTO_COMERCIO, required = false) Comercio comercio,
            Model model) {
        if (comercio == null || !comercio.tieneMesa(1)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("comercio", comercio);
        model.addAttribute("mesas", IntStream.rangeClosed(1, comercio.getCantidadMesas()).boxed().toList());
        return "mesas/imprimir";
    }

    static String urlDeMesa(int numero) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/").queryParam("mesa", numero).build().toUriString();
    }
}
