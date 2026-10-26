package ar.com.vidriera.publico;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.tenant.FiltroComercioPorHost;

/** Imagen para la vista previa del link ({@code og:image}). */
@RestController
public class VistaPreviaController {

    private final GeneradorVistaPrevia generador;

    public VistaPreviaController(GeneradorVistaPrevia generador) {
        this.generador = generador;
    }

    @GetMapping(value = "/vista-previa.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> vistaPrevia(
            @RequestAttribute(name = FiltroComercioPorHost.ATRIBUTO_COMERCIO, required = false) Comercio comercio) {
        if (comercio == null || !comercio.getEstado().sitioPublicoVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(generador.png(comercio));
    }
}
