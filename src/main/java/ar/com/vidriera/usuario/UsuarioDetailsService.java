package ar.com.vidriera.usuario;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Spring Security lo usa en el login para buscar al usuario por email. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;
    private final LimitadorIntentosLogin limitador;

    public UsuarioDetailsService(UsuarioRepository usuarios, LimitadorIntentosLogin limitador) {
        this.usuarios = usuarios;
        this.limitador = limitador;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAutenticado loadUserByUsername(String email) {
        String normalizado;
        try {
            normalizado = Usuario.normalizarEmail(email);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Email vacío");
        }
        Usuario usuario = usuarios.findByEmail(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        boolean habilitado = usuario.isActivo()
                && (usuario.getComercio() == null || usuario.getComercio().getEstado().permiteIngresoAlPanel());
        Long comercioId = usuario.getComercio() == null ? null : usuario.getComercio().getId();
        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getPasswordHash(),
                usuario.getRol(), comercioId, habilitado, limitador.estaBloqueado(normalizado),
                usuario.isDebeCambiarPassword());
    }
}
