package ar.com.vidriera.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import ar.com.vidriera.usuario.Rol;

/**
 * Reglas de acceso. El sitio público es abierto; los paneles requieren login con el rol adecuado.
 * (En la Etapa 3 se restringen los paneles al host de la plataforma y se agrega el login propio.)
 */
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/superadmin/**").hasRole(Rol.SUPER_ADMIN.name())
                        .requestMatchers("/admin/**").hasRole(Rol.DUENO.name())
                        .anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults());
        return http.build();
    }

    /** BCrypt: hash lento y con "sal" aleatoria; dos contraseñas iguales dan hashes distintos. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
