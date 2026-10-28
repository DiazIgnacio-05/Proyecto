package ar.com.vidriera.imagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class ProcesadorImagenesTest {

    private final ProcesadorImagenes procesador = new ProcesadorImagenes();

    @Test
    void generaUnaVersionChicaYUnaGrandeEnJpeg() throws IOException {
        Map<TamanoImagen, byte[]> versiones = procesador.foto(imagen(3000, 2000, "jpg", false));

        BufferedImage chica = leer(versiones.get(TamanoImagen.CHICA));
        BufferedImage grande = leer(versiones.get(TamanoImagen.GRANDE));
        assertThat(chica.getWidth()).isEqualTo(480);
        assertThat(chica.getHeight()).isEqualTo(320);   // mantiene la proporción
        assertThat(grande.getWidth()).isEqualTo(1200);
        assertThat(versiones.get(TamanoImagen.CHICA)).startsWith(0xFF, 0xD8); // encabezado JPEG
    }

    @Test
    void noAgrandaFotosChicas() throws IOException {
        Map<TamanoImagen, byte[]> versiones = procesador.foto(imagen(300, 300, "png", false));
        assertThat(leer(versiones.get(TamanoImagen.GRANDE)).getWidth()).isEqualTo(300);
    }

    @Test
    void loTransparenteQuedaBlancoYNoNegro() throws IOException {
        Map<TamanoImagen, byte[]> versiones = procesador.foto(imagen(600, 600, "png", true));
        int esquina = leer(versiones.get(TamanoImagen.CHICA)).getRGB(2, 2);
        Color color = new Color(esquina);
        assertThat(color.getRed()).isGreaterThan(240);
        assertThat(color.getGreen()).isGreaterThan(240);
        assertThat(color.getBlue()).isGreaterThan(240);
    }

    @Test
    void elLogoQuedaEnPngDentroDe256() throws IOException {
        BufferedImage logo = leer(procesador.logo(imagen(1000, 500, "png", true)));
        assertThat(logo.getWidth()).isEqualTo(256);
        assertThat(logo.getHeight()).isEqualTo(128);
        assertThat(logo.getColorModel().hasAlpha()).isTrue();
    }

    @Test
    void rechazaArchivosQueNoSonImagenes() {
        assertThatThrownBy(() -> procesador.foto("hola, no soy una foto".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ImagenInvalidaException.class)
                .hasMessageContaining("no es una imagen");
        assertThatThrownBy(() -> procesador.foto(new byte[0])).isInstanceOf(ImagenInvalidaException.class);
    }

    @Test
    void rechazaFormatosNoAdmitidos() throws IOException {
        assertThatThrownBy(() -> procesador.foto(imagen(100, 100, "bmp", false)))
                .isInstanceOf(ImagenInvalidaException.class)
                .hasMessageContaining("Formato no admitido");
    }

    @Test
    void rechazaUnaBombaDeDescompresionSinCargarlaEnMemoria() throws IOException {
        // PNG que dice medir 50.000 x 50.000 px (2.500 megapíxeles) pero pesa menos de 100 bytes.
        assertThatThrownBy(() -> procesador.foto(encabezadoPng(50_000, 50_000)))
                .isInstanceOf(ImagenInvalidaException.class)
                .hasMessageContaining("demasiado grande");
    }

    private static byte[] imagen(int ancho, int alto, String formato, boolean transparente) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto,
                transparente ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(new Color(200, 60, 40, 255));
        g.fillOval(ancho / 4, alto / 4, ancho / 2, alto / 2); // el resto queda transparente (o negro)
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }

    private static BufferedImage leer(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /** Firma PNG + bloque IHDR con las dimensiones pedidas (sin datos de imagen). */
    private static byte[] encabezadoPng(int ancho, int alto) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream salida = new DataOutputStream(bytes);
        salida.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        DataOutputStream datos = new DataOutputStream(ihdr);
        datos.writeBytes("IHDR");
        datos.writeInt(ancho);
        datos.writeInt(alto);
        datos.write(new byte[] {8, 2, 0, 0, 0}); // 8 bits, RGB, sin entrelazado
        byte[] bloque = ihdr.toByteArray();
        salida.writeInt(bloque.length - 4);
        salida.write(bloque);
        CRC32 crc = new CRC32();
        crc.update(bloque);
        salida.writeInt((int) crc.getValue());
        return bytes.toByteArray();
    }
}
