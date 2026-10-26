package ar.com.vidriera.tenant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ar.com.vidriera.config.VidrieraProperties;

@Configuration
class TenantConfig {

    @Bean
    ResolutorDeHost resolutorDeHost(VidrieraProperties propiedades) {
        return new ResolutorDeHost(propiedades.dominioBase());
    }
}
