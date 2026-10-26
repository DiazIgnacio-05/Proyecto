package ar.com.vidriera.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.com.vidriera.config.VidrieraProperties;

/**
 * Al arrancar, crea el usuario super-admin si todavía no existe, con el email y la contraseña de
 * las variables de entorno {@code SUPERADMIN_EMAIL} y {@code SUPERADMIN_PASSWORD}. Así la
 * contraseña nunca queda escrita en el código ni en las migraciones.
 */
@Component
public class InicializadorSuperAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorSuperAdmin.class);
    private static final int LARGO_MINIMO_PASSWORD = 12;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final VidrieraProperties.SuperAdmin config;

    public InicializadorSuperAdmin(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
            VidrieraProperties propiedades) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.config = propiedades.superAdmin();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.existsByRol(Rol.SUPER_ADMIN)) {
            return;
        }
        if (config == null || esVacio(config.email()) || esVacio(config.password())) {
            log.warn("No hay super-admin. Definí SUPERADMIN_EMAIL y SUPERADMIN_PASSWORD para crearlo.");
            return;
        }
        if (config.password().length() < LARGO_MINIMO_PASSWORD) {
            throw new IllegalStateException(
                    "SUPERADMIN_PASSWORD debe tener al menos " + LARGO_MINIMO_PASSWORD + " caracteres");
        }
        usuarios.save(Usuario.superAdmin(config.email(), passwordEncoder.encode(config.password()),
                "Super administrador"));
        log.info("Super-admin creado: {}", config.email());
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
