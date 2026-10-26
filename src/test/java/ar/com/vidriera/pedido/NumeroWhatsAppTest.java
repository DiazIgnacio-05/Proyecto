package ar.com.vidriera.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class NumeroWhatsAppTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "3834000001            | 5493834000001",
            "383 4000001           | 5493834000001",
            "0383 4000001          | 5493834000001",
            "(0383) 15-4000001     | 5493834000001",
            "383 15 4000001        | 5493834000001",
            "+54 9 383 400-0001    | 5493834000001",
            "+54 383 4000001       | 5493834000001",
            "11 15 2345 6789       | 5491123456789",
            "011 2345-6789         | 5491123456789",
            "3835 15 412345        | 5493835412345"})
    void normalizaLasFormasHabitualesDeEscribirUnCelular(String escrito, String esperado) {
        assertThat(NumeroWhatsApp.de(escrito).digitos()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "4000001", "383400000", "38340000011234", "abc"})
    void rechazaNumerosIncompletosOInvalidos(String escrito) {
        assertThatThrownBy(() -> NumeroWhatsApp.de(escrito)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elLinkCodificaElMensajeConEspaciosComoPorcentaje20() {
        String link = NumeroWhatsApp.de("3834000001").linkConMensaje("Hola! 2 x Empanada — $ 3.600\nTotal");
        assertThat(link)
                .startsWith("https://wa.me/5493834000001?text=")
                .contains("Hola%21%202%20x%20Empanada")
                .contains("%0ATotal")
                .doesNotContain("+");
    }
}
