package ar.com.vidriera.comercio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ComercioTest {

    private Comercio nuevo(String slug) {
        return new Comercio(slug, "Pizzería Don José", Rubro.RESTAURANTE, "3834123456", LocalDate.of(2026, 10, 1));
    }

    @Test
    void unComercioNuevoEstaActivoYConColoresPorDefecto() {
        Comercio comercio = nuevo("don-jose");
        assertThat(comercio.getEstado()).isEqualTo(EstadoComercio.ACTIVO);
        assertThat(comercio.getApariencia().getColorPrimario()).isEqualTo(Apariencia.COLOR_PRIMARIO_POR_DEFECTO);
        assertThat(comercio.getRedes().tieneAlguna()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Don-Jose", "-donjose", "donjose-", "don jose", "don_jose", "pizzería", ""})
    void rechazaSlugsInvalidos(String slug) {
        assertThatThrownBy(() -> nuevo(slug)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"www", "app", "admin", "api"})
    void rechazaSlugsReservados(String slug) {
        assertThatThrownBy(() -> nuevo(slug))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reservado");
    }

    @Test
    void suspenderYReactivar() {
        Comercio comercio = nuevo("don-jose");
        comercio.suspender();
        assertThat(comercio.getEstado().sitioPublicoVisible()).isFalse();
        assertThat(comercio.getEstado().permiteIngresoAlPanel()).isTrue();
        assertThat(comercio.getEstado().permiteEditar()).isFalse();
        comercio.reactivar();
        assertThat(comercio.getEstado()).isEqualTo(EstadoComercio.ACTIVO);
    }

    @Test
    void noSePuedeSuspenderUnComercioDadoDeBaja() {
        Comercio comercio = nuevo("don-jose");
        comercio.darDeBaja();
        assertThatThrownBy(comercio::suspender).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void modulosContratados() {
        Comercio comercio = nuevo("don-jose");
        assertThat(comercio.tieneModulo(Modulo.DOMINIO_PROPIO)).isFalse();
        comercio.activarModulo(Modulo.DOMINIO_PROPIO);
        assertThat(comercio.tieneModulo(Modulo.DOMINIO_PROPIO)).isTrue();
    }

    @Test
    void rechazaColoresConFormatoInvalido() {
        Comercio comercio = nuevo("don-jose");
        assertThatThrownBy(() -> comercio.getApariencia().setColorPrimario("rojo"))
                .isInstanceOf(IllegalArgumentException.class);
        comercio.getApariencia().setColorPrimario("#a1b2c3");
        assertThat(comercio.getApariencia().getColorPrimario()).isEqualTo("#A1B2C3");
    }
}
