package ar.com.vidriera.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class VarianteTest {

    private final Producto remera = new Producto(new Categoria("Remeras", 1), "Remera básica", new BigDecimal("14000"));

    @Test
    void laEtiquetaCombinaTalleYColor() {
        assertThat(remera.agregarVariante("M", "Negro").etiqueta()).isEqualTo("talle M, Negro");
        assertThat(remera.agregarVariante("XL", null).etiqueta()).isEqualTo("talle XL");
        assertThat(remera.agregarVariante(" ", "Beige").etiqueta()).isEqualTo("Beige");
    }

    @Test
    void noRepiteCombinacionesAunqueCambienMayusculasOEspacios() {
        assertThat(remera.agregarVarianteSiNoExiste("M", "Negro")).isTrue();
        assertThat(remera.agregarVarianteSiNoExiste(" m ", "NEGRO")).isFalse();
        assertThat(remera.agregarVarianteSiNoExiste("M", "Blanco")).isTrue();
        assertThat(remera.getVariantes()).hasSize(2);
    }

    @Test
    void sinNingunTalleConStockNoSePuedePedir() {
        Variante m = remera.agregarVariante("M", null);
        Variante l = remera.agregarVariante("L", null);
        assertThat(remera.sePuedePedir()).isTrue();
        m.setDisponible(false);
        assertThat(remera.sePuedePedir()).isTrue();
        l.setDisponible(false);
        assertThat(remera.sePuedePedir()).isFalse();
    }

    @Test
    void requiereTalleOColor() {
        assertThatThrownBy(() -> remera.agregarVariante("", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
