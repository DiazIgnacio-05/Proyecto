package ar.com.vidriera.comun;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FormatoPesosTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0          | $ 0",
            "950        | $ 950",
            "1800.00    | $ 1.800",
            "15600      | $ 15.600",
            "1234567    | $ 1.234.567",
            "1234.5     | $ 1.234,50",
            "99.999     | $ 100",
            "0.1        | $ 0,10"})
    void formateaAlEstiloArgentino(String importe, String esperado) {
        assertThat(FormatoPesos.formatear(new BigDecimal(importe))).isEqualTo(esperado);
    }

    @org.junit.jupiter.api.Test
    void nullSeMuestraVacio() {
        assertThat(FormatoPesos.formatear(null)).isEmpty();
    }
}
