package ar.com.vidriera.pedido;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Número de celular argentino en el formato internacional que pide WhatsApp: {@code 549} + código de
 * área sin el 0 + número sin el 15 (siempre 13 dígitos).
 *
 * <p>Acepta cómo lo escriben los comercios: "383 4000001", "0383 15 4000001", "+54 9 383 4000001"...
 */
public record NumeroWhatsApp(String digitos) {

    private static final int DIGITOS_NACIONALES = 10;

    public NumeroWhatsApp {
        if (digitos == null || !digitos.matches("549\\d{10}")) {
            throw new IllegalArgumentException("Número de WhatsApp inválido: " + digitos);
        }
    }

    public static NumeroWhatsApp de(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Falta el número de WhatsApp");
        }
        String d = texto.replaceAll("\\D", "");
        if (d.startsWith("549") && d.length() == 13) {
            return new NumeroWhatsApp(d);
        }
        if (d.startsWith("54") && d.length() == 12) {
            d = d.substring(2); // +54 sin el 9 de celular
        }
        if (d.startsWith("0")) {
            d = d.substring(1); // prefijo de larga distancia nacional
        }
        if (d.length() == DIGITOS_NACIONALES + 2) {
            d = quitarQuince(d);
        }
        if (d.length() != DIGITOS_NACIONALES) {
            throw new IllegalArgumentException(
                    "No se reconoce el número '" + texto + "'. Ingresalo con código de área, ej. 383 4123456");
        }
        return new NumeroWhatsApp("549" + d);
    }

    /** El código de área tiene 2, 3 o 4 dígitos; el "15" va justo después. */
    private static String quitarQuince(String d) {
        for (int largoArea = 2; largoArea <= 4; largoArea++) {
            if (d.startsWith("15", largoArea)) {
                return d.substring(0, largoArea) + d.substring(largoArea + 2);
            }
        }
        return d;
    }

    /** Link que abre un chat con este número. */
    public String link() {
        return "https://wa.me/" + digitos;
    }

    /** Link que abre un chat con el mensaje ya escrito. */
    public String linkConMensaje(String mensaje) {
        // URLEncoder codifica los espacios como "+", que WhatsApp no siempre interpreta: se usa %20.
        String codificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8).replace("+", "%20");
        return link() + "?text=" + codificado;
    }
}
