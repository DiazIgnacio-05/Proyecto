package ar.com.vidriera.imagen;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve las imágenes guardadas en disco. Cada archivo tiene un nombre único que nunca se reutiliza
 * (si cambia la foto, cambia el nombre), así que el navegador puede guardarlo en caché un año.
 */
@Configuration
class ImagenesWebConfig implements WebMvcConfigurer {

    private final AlmacenamientoLocal almacenamiento;

    ImagenesWebConfig(AlmacenamientoLocal almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler(AlmacenamientoLocal.PREFIJO_URL + "**")
                .addResourceLocations(almacenamiento.getDirectorio().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
