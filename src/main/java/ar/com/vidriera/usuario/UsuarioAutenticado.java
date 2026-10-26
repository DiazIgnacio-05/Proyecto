package ar.com.vidriera.usuario;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Lo que Spring Security guarda en la sesión del usuario logueado. Lleva el id del comercio para
 * que el panel sepa, sin volver a consultar, cuál es el tenant de cada request.
 */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String email;
    private String passwordHash;
    private final Rol rol;
    private final Long comercioId;
    private final boolean activo;
    private final boolean bloqueado;
    private final boolean debeCambiarPassword;

    public UsuarioAutenticado(Long id, String email, String passwordHash, Rol rol, Long comercioId,
            boolean activo, boolean bloqueado, boolean debeCambiarPassword) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.comercioId = comercioId;
        this.activo = activo;
        this.bloqueado = bloqueado;
        this.debeCambiarPassword = debeCambiarPassword;
    }

    /** Copia del usuario después de cambiar la contraseña (ya no hace falta forzar el cambio). */
    public UsuarioAutenticado conPasswordCambiada() {
        return new UsuarioAutenticado(id, email, null, rol, comercioId, activo, bloqueado, false);
    }

    public Long getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    /** {@code null} para el super-admin. */
    public Long getComercioId() {
        return comercioId;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol.autoridad()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    /** Bloqueado temporalmente por demasiados intentos fallidos (ver {@link LimitadorIntentosLogin}). */
    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    /** Spring Security la llama después del login para no dejar el hash en memoria de sesión. */
    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
