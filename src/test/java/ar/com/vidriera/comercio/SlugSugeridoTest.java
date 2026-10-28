package ar.com.vidriera.comercio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SlugSugeridoTest {

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(delimiter = '|', value = {
            "Pizzería Don José          | pizzeria-don-jose",
            "  La Casona de Ambato!!    | la-casona-de-ambato",
            "Ñandú & Cía.               | nandu-cia",
            "Kiosco 24 hs               | kiosco-24-hs",
            "---                        | ''"})
    void armaUnSubdominioValidoDesdeElNombre(String nombre, String esperado) {
        String slug = Slug.sugerir(nombre);
        assertThat(slug).isEqualTo(esperado);
        if (!slug.isEmpty()) {
            assertThat(Slug.esValido(slug)).isTrue();
        }
    }

    @org.junit.jupiter.api.Test
    void recortaNombresLargosSinDejarGuionAlFinal() {
        String slug = Slug.sugerir("a".repeat(62) + " b c d");
        assertThat(slug).hasSizeLessThanOrEqualTo(Slug.LARGO_MAXIMO).doesNotEndWith("-");
        assertThat(Slug.esValido(slug)).isTrue();
    }
}
