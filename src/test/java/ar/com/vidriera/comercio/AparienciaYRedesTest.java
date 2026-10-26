package ar.com.vidriera.comercio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AparienciaYRedesTest {

    @ParameterizedTest
    @CsvSource({
            "#8C2F1B, #FFFFFF",   // bordó oscuro → texto blanco
            "#1F6F5C, #FFFFFF",
            "#000000, #FFFFFF",
            "#F4B942, #1D1D1F",   // amarillo → texto oscuro
            "#FFFFFF, #1D1D1F",
            "#E9B44C, #1D1D1F"})
    void eligeElColorDeTextoConMejorContraste(String fondo, String texto) {
        assertThat(Apariencia.textoLegibleSobre(fondo)).isEqualTo(texto);
    }

    @Test
    void armaLasUrlsDeRedesDesdeElUsuario() {
        RedesSociales redes = new RedesSociales();
        redes.setInstagram("@nogal.indumentaria");
        redes.setTiktok("nogal");
        assertThat(redes.urlInstagram()).isEqualTo("https://www.instagram.com/nogal.indumentaria");
        assertThat(redes.urlTiktok()).isEqualTo("https://www.tiktok.com/@nogal");
        assertThat(redes.urlFacebook()).isNull();
        assertThat(redes.tieneAlguna()).isTrue();
    }

    @Test
    void respetaUnLinkHttpsCompleto() {
        assertThat(RedesSociales.url("https://www.facebook.com/nogal", "https://www.facebook.com/"))
                .isEqualTo("https://www.facebook.com/nogal");
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "http://inseguro.com", "nogal indumentaria", "<script>"})
    void descartaValoresQueNoSonUnUsuarioNiUnLinkSeguro(String valor) {
        assertThat(RedesSociales.url(valor, "https://www.instagram.com/")).isNull();
    }
}
