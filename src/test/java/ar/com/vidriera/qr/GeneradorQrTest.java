package ar.com.vidriera.qr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

class GeneradorQrTest {

    private final GeneradorQr generador = new GeneradorQr();

    @Test
    void elQrGeneradoSeLeeYDevuelveLaUrlDeLaMesa() throws Exception {
        String url = "https://sabores-del-valle.vidriera.com.ar/?mesa=7";
        assertThat(leer(generador.matriz(url))).isEqualTo(url);
    }

    @Test
    void elSvgTieneFondoBlancoYModulosNegros() {
        String svg = generador.svg("https://ejemplo.test/?mesa=1");
        assertThat(svg)
                .startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\"")
                .contains("fill=\"#fff\"")
                .contains("<path fill=\"#000\" d=\"M");
    }

    /** Dibuja la matriz como imagen en memoria (escalada x4) y la decodifica con el lector de ZXing. */
    private static String leer(BitMatrix matriz) throws Exception {
        int escala = 4;
        int ancho = matriz.getWidth() * escala;
        int alto = matriz.getHeight() * escala;
        int[] pixeles = new int[ancho * alto];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                pixeles[y * ancho + x] = matriz.get(x / escala, y / escala) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        BinaryBitmap imagen = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(ancho, alto, pixeles)));
        return new QRCodeReader().decode(imagen).getText();
    }
}
