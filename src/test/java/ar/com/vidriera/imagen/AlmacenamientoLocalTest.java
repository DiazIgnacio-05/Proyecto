package ar.com.vidriera.imagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ar.com.vidriera.config.VidrieraProperties;

class AlmacenamientoLocalTest {

    @TempDir
    Path carpeta;

    private AlmacenamientoLocal almacenamiento() {
        return new AlmacenamientoLocal(new VidrieraProperties("localhost", ZoneId.of("UTC"), null,
                new VidrieraProperties.Imagenes(carpeta.toString())));
    }

    @Test
    void guardaBorraYArmaLaUrl() {
        AlmacenamientoLocal a = almacenamiento();
        a.guardar("c3/productos/abc-123-480.jpg", new byte[] {1, 2, 3});
        assertThat(carpeta.resolve("c3/productos/abc-123-480.jpg")).exists();
        assertThat(a.url("c3/productos/abc-123-480.jpg")).isEqualTo("/imagenes/c3/productos/abc-123-480.jpg");
        a.eliminar("c3/productos/abc-123-480.jpg");
        assertThat(Files.exists(carpeta.resolve("c3/productos/abc-123-480.jpg"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../fuera.jpg", "c3/../../fuera.jpg", "/etc/passwd", "c3\\x.jpg", "c3/x.exe", "C3/x.jpg"})
    void rechazaRutasPeligrosas(String ruta) {
        assertThatThrownBy(() -> almacenamiento().guardar(ruta, new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
