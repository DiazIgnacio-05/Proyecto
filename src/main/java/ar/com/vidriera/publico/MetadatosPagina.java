package ar.com.vidriera.publico;

import ar.com.vidriera.comercio.Comercio;

/**
 * Datos para buscadores y para la vista previa al compartir el link (WhatsApp, Facebook, etc. leen
 * las etiquetas "Open Graph" {@code og:title}, {@code og:description} y {@code og:image}).
 *
 * @param urlBase ej. {@code https://sabores-del-valle.vidriera.com.ar}, sin barra final
 */
public record MetadatosPagina(String titulo, String descripcion, String urlCanonica, String urlImagen) {

    static final int LARGO_DESCRIPCION = 160;

    public static MetadatosPagina de(Comercio comercio, String urlBase) {
        String titulo = primeroNoVacio(comercio.getSeo().getTitulo(), comercio.getNombre());
        String descripcion = primeroNoVacio(comercio.getSeo().getDescripcion(), comercio.getDescripcion(),
                "Mirá los productos de " + comercio.getNombre() + " y hacé tu pedido por WhatsApp.");
        String portada = comercio.getApariencia().getPortadaUrl();
        String imagen = portada != null ? absoluta(portada, urlBase) : urlBase + "/vista-previa.png";
        return new MetadatosPagina(titulo, recortar(descripcion), urlBase + "/", imagen);
    }

    private static String absoluta(String url, String urlBase) {
        return url.startsWith("http") ? url : urlBase + (url.startsWith("/") ? "" : "/") + url;
    }

    private static String recortar(String texto) {
        return texto.length() <= LARGO_DESCRIPCION ? texto : texto.substring(0, LARGO_DESCRIPCION - 1) + "…";
    }

    private static String primeroNoVacio(String... opciones) {
        for (String opcion : opciones) {
            if (opcion != null && !opcion.isBlank()) {
                return opcion.strip();
            }
        }
        return "";
    }
}
