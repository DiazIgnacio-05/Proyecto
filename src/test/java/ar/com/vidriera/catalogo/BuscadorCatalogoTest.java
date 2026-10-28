package ar.com.vidriera.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BuscadorCatalogoTest {

    @Test
    void ignoraMayusculasTildesYSignos() {
        assertThat(BuscadorCatalogo.palabras("  Pava   ELÉCTRICA!! ")).containsExactly("pava", "electrica");
        assertThat(BuscadorCatalogo.coincide(BuscadorCatalogo.palabras("pava electrica"), "Pava eléctrica con corte"))
                .isTrue();
    }

    @Test
    void exigeTodasLasPalabrasEnCualquierCampo() {
        List<String> palabras = BuscadorCatalogo.palabras("termo acero");
        assertThat(BuscadorCatalogo.coincide(palabras, "Termo 1 L", "Doble pared de acero", "Mate")).isTrue();
        assertThat(BuscadorCatalogo.coincide(palabras, "Termo 1 L", "Doble pared", "Mate")).isFalse();
    }

    @Test
    void encuentraPartesDePalabras() {
        assertThat(BuscadorCatalogo.coincide(BuscadorCatalogo.palabras("rem"), "Remera básica")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "!!!"})
    void unaBusquedaVaciaCoincideConTodo(String busqueda) {
        assertThat(BuscadorCatalogo.palabras(busqueda)).isEmpty();
        assertThat(BuscadorCatalogo.coincide(BuscadorCatalogo.palabras(busqueda), "Cualquier cosa")).isTrue();
    }

    @Test
    void toleraCamposNulosYBusquedasLarguisimas() {
        assertThat(BuscadorCatalogo.coincide(BuscadorCatalogo.palabras("mate"), "Mate", null)).isTrue();
        assertThat(String.join(" ", BuscadorCatalogo.palabras("a".repeat(500))))
                .hasSize(BuscadorCatalogo.LARGO_MAXIMO);
    }
}
