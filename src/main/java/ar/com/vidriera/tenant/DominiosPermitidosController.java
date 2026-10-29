package ar.com.vidriera.tenant;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.EstadoComercio;
import ar.com.vidriera.comercio.Modulo;

/**
 * Consulta que hace Caddy antes de pedir un certificado HTTPS para un dominio propio ("on-demand TLS").
 * Responde 200 solo para dominios dados de alta en el super-admin; así nadie puede hacer que el
 * servidor pida certificados para dominios cualquiera apuntándolos a nuestra IP.
 * Caddy la llama por la red interna de Docker; desde afuera {@code /interno/**} da 404 (ver Caddyfile).
 */
@RestController
public class DominiosPermitidosController {

    public static final String RUTA = "/interno/dominio-permitido";

    private final ComercioRepository comercios;

    public DominiosPermitidosController(ComercioRepository comercios) {
        this.comercios = comercios;
    }

    @GetMapping(RUTA)
    public ResponseEntity<Void> permitido(@RequestParam(name = "domain", required = false) String dominio) {
        if (dominio == null || dominio.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean habilitado = comercios.existeDominioHabilitado(dominio.strip().toLowerCase(Locale.ROOT),
                Modulo.DOMINIO_PROPIO, EstadoComercio.BAJA);
        return habilitado ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
