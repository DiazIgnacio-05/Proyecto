package ar.com.vidriera.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone un {@link Clock} con la zona horaria de los comercios. El código pide la hora a este
 * reloj en vez de llamar a {@code LocalDateTime.now()}, así en los tests se puede fijar la hora.
 */
@Configuration
class RelojConfig {

    @Bean
    Clock reloj(VidrieraProperties propiedades) {
        return Clock.system(propiedades.zonaHoraria());
    }
}
