package ar.com.vidriera.comun;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LectorPesosTest {

    @ParameterizedTest(name = "\"{0}\" = {1}")
    @CsvSource(delimiter = '|', value = {
            "12500        | 12500",
            "12.500       | 12500",
            "$ 12.500     | 12500",
            "1.234.567    | 1234567",
            "1.250,50     | 1250.50",
            "1250,5       | 1250.5",
            "1250.5       | 1250.5",
            "12.50        | 12.50",
            "0            | 0",
            "' 950 '      | 950"})
    void entiendeComoEscribeLosPreciosUnaPersonaEnArgentina(String texto, String esperado) {
        assertThat(LectorPesos.leer(texto)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo(esperado));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "-500", "12,500,00", "1.2.3", "12.5000", "99999999999"})
    void rechazaTextosQueNoSonPrecios(String texto) {
        assertThat(LectorPesos.leer(texto)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"12500, 12500", "12500.00, 12500", "1250.50, '1250,50'", "1250.5, '1250,50'"})
    void muestraElPrecioParaEditarSinPuntosDeMiles(String importe, String esperado) {
        assertThat(LectorPesos.paraEditar(new BigDecimal(importe))).isEqualTo(esperado);
    }
}
