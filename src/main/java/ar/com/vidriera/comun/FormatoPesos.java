package ar.com.vidriera.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.springframework.stereotype.Component;

/**
 * Formatea precios en pesos al estilo argentino: {@code $ 15.600} o {@code $ 1.234,50}.
 * Los centavos se muestran solo si existen.
 *
 * <p>Es un bean llamado "pesos" para poder usarlo desde Thymeleaf: {@code ${@pesos.de(producto.precio)}}.
 */
@Component("pesos")
public class FormatoPesos {

    private static final DecimalFormatSymbols SIMBOLOS = simbolos();

    public static String formatear(BigDecimal importe) {
        if (importe == null) {
            return "";
        }
        BigDecimal redondeado = importe.setScale(2, RoundingMode.HALF_UP);
        boolean tieneCentavos = redondeado.remainder(BigDecimal.ONE).signum() != 0;
        // DecimalFormat no es thread-safe: se crea uno por llamada (es barato).
        DecimalFormat formato = new DecimalFormat(tieneCentavos ? "#,##0.00" : "#,##0", SIMBOLOS);
        return "$ " + formato.format(redondeado);
    }

    /** Versión de instancia, para Thymeleaf. */
    public String de(BigDecimal importe) {
        return formatear(importe);
    }

    private static DecimalFormatSymbols simbolos() {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        simbolos.setMinusSign('-');
        return simbolos;
    }
}
