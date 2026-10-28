package ar.com.vidriera.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ListaDeVariantesTest {

    @Test
    void separaPorComaOPuntoYComaYSacaRepetidos() {
        assertThat(ProductoAdminService.lista(" S, M ;l, m,, XL ", 20, "talle")).containsExactly("S", "M", "l", "XL");
    }

    @Test
    void textoVacioEsListaVacia() {
        assertThat(ProductoAdminService.lista("  ", 20, "talle")).isEmpty();
        assertThat(ProductoAdminService.lista(null, 20, "talle")).isEmpty();
    }

    @Test
    void rechazaValoresDemasiadoLargos() {
        assertThatThrownBy(() -> ProductoAdminService.lista("Verde agua con lunares blancos y rojos", 30, "color"))
                .isInstanceOf(OperacionRechazadaException.class)
                .hasMessageContaining("color");
    }
}
