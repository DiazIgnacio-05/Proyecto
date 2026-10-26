package ar.com.vidriera.comun;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Interpreta un precio como lo escribe una persona en Argentina: "12500", "12.500", "$ 12.500",
 * "1.250,50" o "1250,5". El punto es separador de miles y la coma, de decimales.
 */
public final class LectorPesos {

    /** Miles con punto: "12.500", "1.234.567". */
    private static final Pattern CON_MILES = Pattern.compile("^\\d{1,3}(\\.\\d{3})+(,\\d{1,2})?$");
    private static final Pattern SIMPLE = Pattern.compile("^\\d+([.,]\\d{1,2})?$");
    static final BigDecimal MAXIMO = new BigDecimal("9999999999.99");

    private LectorPesos() {
    }

    /** Vacío si el texto no es un importe válido (o es negativo o demasiado grande). */
    public static Optional<BigDecimal> leer(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpio = texto.replace("$", "").replaceAll("\\s", "");
        String normalizado;
        if (CON_MILES.matcher(limpio).matches()) {
            normalizado = limpio.replace(".", "").replace(',', '.');
        } else if (SIMPLE.matcher(limpio).matches()) {
            normalizado = limpio.replace(',', '.');
        } else {
            return Optional.empty();
        }
        BigDecimal importe = new BigDecimal(normalizado);
        return importe.compareTo(MAXIMO) > 0 ? Optional.empty() : Optional.of(importe);
    }

    /** Para mostrar un precio en un campo de formulario: "12500" o "1250,50" (sin "$" ni puntos). */
    public static String paraEditar(BigDecimal importe) {
        if (importe == null) {
            return "";
        }
        BigDecimal sinCerosDeMas = importe.stripTrailingZeros();
        if (sinCerosDeMas.scale() <= 0) {
            return sinCerosDeMas.toPlainString();
        }
        return importe.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
