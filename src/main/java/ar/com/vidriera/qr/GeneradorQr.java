package ar.com.vidriera.qr;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Genera códigos QR como SVG (imagen vectorial: se imprime nítida en cualquier tamaño y pesa poco).
 * ZXing calcula la matriz de módulos blancos y negros; acá solo la dibujamos.
 */
@Component
public class GeneradorQr {

    /** Margen blanco alrededor del código, en módulos. La norma pide 4; 2 alcanza para impresión. */
    private static final int MARGEN = 2;

    public BitMatrix matriz(String contenido) {
        try {
            // Nivel M: tolera ~15% de daño (manchas, dobleces), suficiente para una mesa.
            return new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, MARGEN,
                            EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalArgumentException("No se pudo generar el QR para: " + contenido, e);
        }
    }

    public String svg(String contenido) {
        BitMatrix matriz = matriz(contenido);
        int ancho = matriz.getWidth();
        int alto = matriz.getHeight();
        StringBuilder trazo = new StringBuilder();
        for (int y = 0; y < alto; y++) {
            int x = 0;
            while (x < ancho) {
                if (!matriz.get(x, y)) {
                    x++;
                    continue;
                }
                // Se agrupan los módulos negros consecutivos de la fila en un solo rectángulo.
                int inicio = x;
                while (x < ancho && matriz.get(x, y)) {
                    x++;
                }
                trazo.append('M').append(inicio).append(' ').append(y)
                        .append('h').append(x - inicio).append("v1h-").append(x - inicio).append('z');
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + ancho + " " + alto
                + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<path fill=\"#000\" d=\"" + trazo + "\"/></svg>";
    }
}
