package ar.com.vidriera.imagen;

/**
 * Dónde se guardan los archivos de imagen. Es una interfaz para poder cambiar el lugar sin tocar
 * el resto del código: hoy disco local ({@link AlmacenamientoLocal}); más adelante, por ejemplo,
 * Cloudflare R2.
 *
 * <p>La "ruta" es relativa, del estilo {@code c3/productos/9f2c...-480.jpg}.
 */
public interface AlmacenamientoImagenes {

    void guardar(String ruta, byte[] contenido);

    /** No falla si el archivo no existe. */
    void eliminar(String ruta);

    /** URL con la que el navegador pide el archivo. */
    String url(String ruta);
}
