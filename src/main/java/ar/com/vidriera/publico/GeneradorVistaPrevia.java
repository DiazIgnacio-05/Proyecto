package ar.com.vidriera.publico;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import ar.com.vidriera.comercio.Comercio;

/**
 * Dibuja la imagen de 1200x630 px que muestran WhatsApp y las redes al compartir el link del
 * comercio (si no cargó una portada propia). Usa Java2D, la API de dibujo que trae el JDK: no
 * necesita librerías ni servicios externos. WhatsApp no acepta SVG, por eso es un PNG.
 */
@Component
public class GeneradorVistaPrevia {

    static final int ANCHO = 1200;
    static final int ALTO = 630;
    private static final int MARGEN = 90;

    public byte[] png(Comercio comercio) {
        BufferedImage imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(Color.decode(comercio.getApariencia().getColorPrimario()));
            g.fillRect(0, 0, ANCHO, ALTO);
            g.setColor(Color.decode(comercio.getApariencia().getColorSecundario()));
            g.fillRect(0, ALTO - 24, ANCHO, 24);

            Color texto = Color.decode(comercio.getApariencia().getColorSobrePrimario());
            g.setColor(texto);
            int anchoUtil = ANCHO - 2 * MARGEN;

            Font fuenteNombre = ajustar(g, comercio.getNombre(), Font.BOLD, 96, 48, anchoUtil);
            g.setFont(fuenteNombre);
            g.drawString(recortar(g, comercio.getNombre(), anchoUtil), MARGEN, 290);

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 40));
            g.drawString(recortar(g, comercio.getRubro().getEtiqueta(), anchoUtil), MARGEN, 370);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
            g.drawString("Mirá el menú y pedí por WhatsApp", MARGEN, 500);
        } finally {
            g.dispose();
        }
        return aPng(imagen);
    }

    /** Achica la fuente hasta que el texto entre en el ancho (sin bajar del tamaño mínimo). */
    private static Font ajustar(Graphics2D g, String texto, int estilo, int maximo, int minimo, int ancho) {
        for (int tamano = maximo; tamano > minimo; tamano -= 4) {
            Font fuente = new Font(Font.SANS_SERIF, estilo, tamano);
            if (g.getFontMetrics(fuente).stringWidth(texto) <= ancho) {
                return fuente;
            }
        }
        return new Font(Font.SANS_SERIF, estilo, minimo);
    }

    /** Si ni con la fuente mínima entra, corta el texto y agrega "…". */
    private static String recortar(Graphics2D g, String texto, int ancho) {
        FontMetrics medidas = g.getFontMetrics();
        if (medidas.stringWidth(texto) <= ancho) {
            return texto;
        }
        String recortado = texto;
        while (!recortado.isEmpty() && medidas.stringWidth(recortado + "…") > ancho) {
            recortado = recortado.substring(0, recortado.length() - 1);
        }
        return recortado.strip() + "…";
    }

    private static byte[] aPng(BufferedImage imagen) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
