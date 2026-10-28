package ar.com.vidriera.comercio;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import ar.com.vidriera.config.VidrieraProperties;

/**
 * Arma la dirección pública del sitio de un comercio. Hace falta en el panel (que vive en
 * {@code app.<dominio>}) para los QR de mesa y el link "Ver mi sitio".
 */
@Component
public class UrlsDelComercio {

    private final String dominioBase;

    public UrlsDelComercio(VidrieraProperties propiedades) {
        this.dominioBase = propiedades.dominioBase();
    }

    /** Ej. {@code https://sabores-del-valle.vidriera.com.ar}. Usa el esquema y el puerto del request actual. */
    public String sitio(Comercio comercio, HttpServletRequest request) {
        String host = comercio.tieneModulo(Modulo.DOMINIO_PROPIO) && comercio.getDominioPropio() != null
                ? comercio.getDominioPropio()
                : comercio.getSlug() + "." + dominioBase;
        int puerto = request.getServerPort();
        boolean puertoPorDefecto = puerto == 80 && "http".equals(request.getScheme())
                || puerto == 443 && "https".equals(request.getScheme());
        return request.getScheme() + "://" + host + (puertoPorDefecto ? "" : ":" + puerto);
    }

    public String mesa(Comercio comercio, int numero, HttpServletRequest request) {
        return sitio(comercio, request) + "/?mesa=" + numero;
    }
}
