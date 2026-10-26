package ar.com.vidriera.publico;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.Rubro;

class MetadatosPaginaTest {

    private static final String BASE = "https://don-jose.vidriera.com.ar";

    private final Comercio comercio =
            new Comercio("don-jose", "Pizzería Don José", Rubro.RESTAURANTE, "3834123456", LocalDate.of(2026, 1, 1));

    @Test
    void sinDatosSeoUsaNombreYUnaDescripcionGenerica() {
        MetadatosPagina meta = MetadatosPagina.de(comercio, BASE);
        assertThat(meta.titulo()).isEqualTo("Pizzería Don José");
        assertThat(meta.descripcion()).contains("Pizzería Don José").contains("WhatsApp");
        assertThat(meta.urlCanonica()).isEqualTo(BASE + "/");
        assertThat(meta.urlImagen()).isEqualTo(BASE + "/vista-previa.png");
    }

    @Test
    void losDatosSeoCargadosTienenPrioridad() {
        comercio.getSeo().setTitulo("Don José · Pizzas a la piedra");
        comercio.setDescripcion("Pizzas y empanadas");
        comercio.getApariencia().setPortadaUrl("/imagenes/portada.jpg");
        MetadatosPagina meta = MetadatosPagina.de(comercio, BASE);
        assertThat(meta.titulo()).isEqualTo("Don José · Pizzas a la piedra");
        assertThat(meta.descripcion()).isEqualTo("Pizzas y empanadas");
        assertThat(meta.urlImagen()).isEqualTo(BASE + "/imagenes/portada.jpg");
    }

    @Test
    void recortaDescripcionesLargas() {
        comercio.setDescripcion("x".repeat(300));
        assertThat(MetadatosPagina.de(comercio, BASE).descripcion())
                .hasSize(MetadatosPagina.LARGO_DESCRIPCION)
                .endsWith("…");
    }
}
