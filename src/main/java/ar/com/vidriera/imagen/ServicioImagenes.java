package ar.com.vidriera.imagen;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Punto de entrada para todo lo que tiene que ver con imágenes: procesar, guardar, armar URLs y
 * borrar. Las carpetas van por comercio ({@code c<id>/...}) y los nombres son aleatorios.
 *
 * <p>Una foto de producto se guarda con una "clave" ({@code c3/productos/<uuid>}) y un archivo por
 * tamaño ({@code <clave>-480.jpg}, {@code <clave>-1200.jpg}). Logo y portada se guardan como URL.
 */
@Service
public class ServicioImagenes {

    private static final Logger log = LoggerFactory.getLogger(ServicioImagenes.class);

    private final ProcesadorImagenes procesador;
    private final AlmacenamientoImagenes almacenamiento;

    public ServicioImagenes(ProcesadorImagenes procesador, AlmacenamientoImagenes almacenamiento) {
        this.procesador = procesador;
        this.almacenamiento = almacenamiento;
    }

    /** Procesa y guarda una foto de producto. Devuelve la clave para guardar en la base. */
    public String guardarFotoDeProducto(Long comercioId, byte[] original) {
        String clave = "c" + comercioId + "/productos/" + UUID.randomUUID();
        guardarVersiones(clave, procesador.foto(original));
        return clave;
    }

    public String urlFoto(String clave, TamanoImagen tamano) {
        return almacenamiento.url(clave + tamano.sufijo());
    }

    public void eliminarFoto(String clave) {
        for (TamanoImagen tamano : TamanoImagen.values()) {
            eliminarSinFallar(clave + tamano.sufijo());
        }
    }

    /** Devuelve la URL del logo (PNG). */
    public String guardarLogo(Long comercioId, byte[] original) {
        String ruta = "c" + comercioId + "/logo-" + UUID.randomUUID() + ".png";
        almacenamiento.guardar(ruta, procesador.logo(original));
        return almacenamiento.url(ruta);
    }

    /** Devuelve la URL de la portada (versión grande; también se usa como imagen al compartir). */
    public String guardarPortada(Long comercioId, byte[] original) {
        String clave = "c" + comercioId + "/portada-" + UUID.randomUUID();
        guardarVersiones(clave, procesador.foto(original));
        return urlFoto(clave, TamanoImagen.GRANDE);
    }

    /** Borra un logo o una portada a partir de su URL (por ejemplo, al reemplazarlos). */
    public void eliminarPorUrl(String url) {
        String prefijo = almacenamiento.url("");
        if (url == null || !url.startsWith(prefijo)) {
            return;
        }
        String ruta = url.substring(prefijo.length());
        if (ruta.endsWith(TamanoImagen.GRANDE.sufijo())) {
            eliminarFoto(ruta.substring(0, ruta.length() - TamanoImagen.GRANDE.sufijo().length()));
        } else {
            eliminarSinFallar(ruta);
        }
    }

    private void guardarVersiones(String clave, Map<TamanoImagen, byte[]> versiones) {
        versiones.forEach((tamano, contenido) -> almacenamiento.guardar(clave + tamano.sufijo(), contenido));
    }

    /** Si un archivo no se puede borrar no se corta la operación: solo queda un archivo huérfano. */
    private void eliminarSinFallar(String ruta) {
        try {
            almacenamiento.eliminar(ruta);
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar la imagen {}: {}", ruta, e.getMessage());
        }
    }
}
