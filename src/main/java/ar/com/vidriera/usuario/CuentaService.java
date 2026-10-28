package ar.com.vidriera.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operaciones sobre la cuenta del usuario logueado. */
@Service
public class CuentaService {

    public static final int LARGO_MINIMO_PASSWORD = 10;

    /** El cambio de contraseña no se pudo hacer; el mensaje se le muestra al usuario. */
    public static class CambioRechazadoException extends RuntimeException {
        public CambioRechazadoException(String mensaje) {
            super(mensaje);
        }
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public CuentaService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void cambiarPassword(Long usuarioId, String actual, String nueva, String repetida) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Usuario inexistente: " + usuarioId));
        if (actual == null || !passwordEncoder.matches(actual, usuario.getPasswordHash())) {
            throw new CambioRechazadoException("La contraseña actual no es correcta");
        }
        if (nueva == null || nueva.length() < LARGO_MINIMO_PASSWORD) {
            throw new CambioRechazadoException(
                    "La contraseña nueva tiene que tener al menos " + LARGO_MINIMO_PASSWORD + " caracteres");
        }
        if (!nueva.equals(repetida)) {
            throw new CambioRechazadoException("Las dos contraseñas nuevas no coinciden");
        }
        if (nueva.equals(actual)) {
            throw new CambioRechazadoException("La contraseña nueva tiene que ser distinta de la actual");
        }
        if (nueva.equalsIgnoreCase(usuario.getEmail())) {
            throw new CambioRechazadoException("No uses tu email como contraseña");
        }
        usuario.cambiarPassword(passwordEncoder.encode(nueva));
    }
}
