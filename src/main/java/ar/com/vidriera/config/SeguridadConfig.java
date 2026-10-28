package ar.com.vidriera.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import ar.com.vidriera.usuario.Rol;
import ar.com.vidriera.usuario.UsuarioAutenticado;

/**
 * Reglas de acceso. El sitio público es abierto; los paneles requieren login con el rol adecuado.
 * Los paneles y el login solo existen en el host de la plataforma (ver FiltroComercioPorHost).
 */
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, SecurityContextRepository contextos) throws Exception {
        http
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/superadmin/**").hasRole(Rol.SUPER_ADMIN.name())
                        .requestMatchers("/admin/**").hasRole(Rol.DUENO.name())
                        .anyRequest().permitAll())
                // CSRF protege acciones que cambian algo en nombre de un usuario logueado. El envío
                // del pedido no guarda nada ni usa sesión (solo arma un link de WhatsApp), y pedir un
                // token obligaría a crear una sesión por cada visitante del sitio público.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/pedido"))
                .securityContext(contexto -> contexto.securityContextRepository(contextos))
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(alEntrar())
                        .failureHandler(alFallar())
                        .permitAll())
                .logout(salida -> salida.logoutSuccessUrl("/login?salir"));
        return http.build();
    }

    /** Cada rol entra a su panel. */
    private static AuthenticationSuccessHandler alEntrar() {
        return (request, response, autenticacion) -> {
            boolean esSuperAdmin = autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario
                    && usuario.getRol() == Rol.SUPER_ADMIN;
            response.sendRedirect(esSuperAdmin ? "/superadmin" : "/admin");
        };
    }

    /** El mensaje de error no dice si el email existe: así no se pueden averiguar cuentas. */
    private static AuthenticationFailureHandler alFallar() {
        return (request, response, error) -> {
            String motivo = switch (error) {
                case LockedException e -> "bloqueado";
                case DisabledException e -> "deshabilitado";
                default -> "error";
            };
            response.sendRedirect("/login?" + motivo);
        };
    }

    /** Dónde se guarda el usuario logueado entre requests (la sesión HTTP). */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** BCrypt: hash lento y con "sal" aleatoria; dos contraseñas iguales dan hashes distintos. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
