package ar.com.vidriera.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GeneradorContrasenasTest {

    private final GeneradorContrasenas generador = new GeneradorContrasenas();

    @Test
    void tieneTresBloquesDeCuatroSinCaracteresConfusos() {
        for (int i = 0; i < 200; i++) {
            String clave = generador.generar();
            assertThat(clave).matches("[a-km-np-z2-9]{4}-[a-km-np-z2-9]{4}-[a-km-np-z2-9]{4}");
            assertThat(clave.length()).isGreaterThanOrEqualTo(CuentaService.LARGO_MINIMO_PASSWORD);
        }
    }

    @Test
    void noSeRepiten() {
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertThat(vistas.add(generador.generar())).isTrue();
        }
    }
}
