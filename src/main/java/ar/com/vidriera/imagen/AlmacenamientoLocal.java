package ar.com.vidriera.imagen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ar.com.vidriera.config.VidrieraProperties;

/**
 * Guarda las imágenes en una carpeta del disco. Las sirve {@link ImagenesWebConfig} bajo
 * {@value #PREFIJO_URL}.
 */
@Component
public class AlmacenamientoLocal implements AlmacenamientoImagenes {

    static final String PREFIJO_URL = "/imagenes/";

    /** Solo letras, números, guiones y barras: impide rutas como "../../etc/passwd". */
    private static final Pattern RUTA_SEGURA = Pattern.compile("^[a-z0-9]+(/[a-z0-9-]+)*\\.(jpg|png)$");

    private final Path directorio;

    public AlmacenamientoLocal(VidrieraProperties propiedades) {
        this.directorio = Path.of(propiedades.imagenes().directorio()).toAbsolutePath().normalize();
    }

    @Override
    public void guardar(String ruta, byte[] contenido) {
        Path destino = resolver(ruta);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen " + ruta, e);
        }
    }

    @Override
    public void eliminar(String ruta) {
        try {
            Files.deleteIfExists(resolver(ruta));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar la imagen " + ruta, e);
        }
    }

    @Override
    public String url(String ruta) {
        return PREFIJO_URL + ruta;
    }

    Path getDirectorio() {
        return directorio;
    }

    private Path resolver(String ruta) {
        if (ruta == null || !RUTA_SEGURA.matcher(ruta).matches()) {
            throw new IllegalArgumentException("Ruta de imagen inválida: " + ruta);
        }
        return directorio.resolve(ruta);
    }
}
