package ar.com.vidriera.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResolutorDeHostTest {

    private final ResolutorDeHost produccion = new ResolutorDeHost("vidriera.com.ar");
    private final ResolutorDeHost desarrollo = new ResolutorDeHost("localhost");

    @Test
    void unSubdominioEsElSitioDeUnComercio() {
        assertThat(produccion.resolver("sabores-del-valle.vidriera.com.ar"))
                .isEqualTo(new DestinoHost.SitioDeComercio("sabores-del-valle"));
    }

    @Test
    void enDesarrolloFuncionaConPuntoLocalhost() {
        assertThat(desarrollo.resolver("nogal.localhost")).isEqualTo(new DestinoHost.SitioDeComercio("nogal"));
        assertThat(desarrollo.resolver("localhost")).isInstanceOf(DestinoHost.Plataforma.class);
    }

    @Test
    void ignoraMayusculasYPuntoFinal() {
        assertThat(produccion.resolver("Nogal.Vidriera.com.ar."))
                .isEqualTo(new DestinoHost.SitioDeComercio("nogal"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"vidriera.com.ar", "www.vidriera.com.ar", "app.vidriera.com.ar",
            "a.b.vidriera.com.ar", "127.0.0.1", "[::1]", ""})
    void dominioBaseSubdominiosReservadosEIpsSonLaPlataforma(String host) {
        assertThat(produccion.resolver(host)).isInstanceOf(DestinoHost.Plataforma.class);
    }

    @Test
    void otroDominioSeTrataComoDominioPropio() {
        assertThat(produccion.resolver("www.saboresdelvalle.com"))
                .isEqualTo(new DestinoHost.DominioPropio("www.saboresdelvalle.com"));
    }

    @Test
    void unDominioQueSoloTerminaIgualNoEsSubdominio() {
        // "otravidriera.com.ar" termina en "vidriera.com.ar" pero no es un subdominio nuestro.
        assertThat(produccion.resolver("otravidriera.com.ar"))
                .isEqualTo(new DestinoHost.DominioPropio("otravidriera.com.ar"));
    }
}
