package ar.com.vidriera.imagen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

import net.coobird.thumbnailator.Thumbnails;

/**
 * Convierte la foto que sube el comercio (que puede pesar varios MB) en versiones livianas para
 * celular. Además:
 * <ul>
 *   <li>respeta la orientación con la que se sacó la foto (dato EXIF del celular);</li>
 *   <li>elimina los metadatos, incluida la ubicación GPS que guardan muchos celulares;</li>
 *   <li>rechaza archivos que no son imágenes o que son enormes (protección contra
 *       "bombas de descompresión": archivos chicos que al abrirse ocupan gigas de memoria).</li>
 * </ul>
 */
@Component
public class ProcesadorImagenes {

    static final int PIXELES_MAXIMOS = 40_000_000; // ~ foto de 40 megapíxeles
    static final int LADO_LOGO = 256;
    private static final float CALIDAD_JPEG = 0.8f;
    private static final Set<String> FORMATOS = Set.of("jpeg", "png");

    /** Una foto en todos los tamaños de {@link TamanoImagen}, en JPEG. */
    public Map<TamanoImagen, byte[]> foto(byte[] original) {
        BufferedImage imagen = sobreFondoBlanco(leer(original));
        Map<TamanoImagen, byte[]> versiones = new EnumMap<>(TamanoImagen.class);
        for (TamanoImagen tamano : TamanoImagen.values()) {
            versiones.put(tamano, jpeg(imagen, Math.min(tamano.getAncho(), imagen.getWidth())));
        }
        return versiones;
    }

    /** Logo en PNG (mantiene la transparencia), dentro de un cuadrado de {@value #LADO_LOGO} px. */
    public byte[] logo(byte[] original) {
        BufferedImage imagen = leer(original);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            int lado = Math.min(LADO_LOGO, Math.max(imagen.getWidth(), imagen.getHeight()));
            Thumbnails.of(imagen).size(lado, lado).outputFormat("png").toOutputStream(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new ImagenInvalidaException("No se pudo procesar el logo");
        }
    }

    /** Valida formato y tamaño antes de decodificar, y aplica la orientación EXIF. */
    BufferedImage leer(byte[] original) {
        if (original == null || original.length == 0) {
            throw new ImagenInvalidaException("El archivo está vacío");
        }
        validarSinDecodificar(original);
        try {
            // Leer desde el stream (y no con ImageIO.read) permite que Thumbnailator use el EXIF.
            return Thumbnails.of(new ByteArrayInputStream(original)).scale(1).asBufferedImage();
        } catch (IOException | RuntimeException e) {
            throw new ImagenInvalidaException("No pudimos abrir la imagen. Probá con otra foto (JPG o PNG).");
        }
    }

    /** Lee solo el encabezado: formato y dimensiones, sin cargar la imagen en memoria. */
    private static void validarSinDecodificar(byte[] original) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new ImagenInvalidaException("El archivo no es una imagen. Subí una foto JPG o PNG.");
            }
            ImageReader lector = lectores.next();
            try {
                String formato = lector.getFormatName().toLowerCase();
                if (!FORMATOS.contains(formato)) {
                    throw new ImagenInvalidaException("Formato no admitido (" + formato + "). Subí una foto JPG o PNG.");
                }
                lector.setInput(entrada);
                long pixeles = (long) lector.getWidth(0) * lector.getHeight(0);
                if (pixeles > PIXELES_MAXIMOS) {
                    throw new ImagenInvalidaException("La imagen es demasiado grande. Probá con una foto más chica.");
                }
            } finally {
                lector.dispose();
            }
        } catch (IOException e) {
            throw new ImagenInvalidaException("No pudimos leer la imagen. Probá con otra foto.");
        }
    }

    /** JPEG no tiene transparencia: lo transparente (ej. un PNG) queda blanco y no negro. */
    private static BufferedImage sobreFondoBlanco(BufferedImage imagen) {
        if (!imagen.getColorModel().hasAlpha()) {
            return imagen;
        }
        BufferedImage opaca = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = opaca.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imagen.getWidth(), imagen.getHeight());
            g.drawImage(imagen, 0, 0, null);
        } finally {
            g.dispose();
        }
        return opaca;
    }

    private static byte[] jpeg(BufferedImage imagen, int ancho) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Thumbnails.of(imagen).width(ancho).outputFormat("jpg").outputQuality(CALIDAD_JPEG).toOutputStream(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new ImagenInvalidaException("No se pudo procesar la imagen");
        }
    }
}
