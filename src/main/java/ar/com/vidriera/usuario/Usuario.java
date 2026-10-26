package ar.com.vidriera.usuario;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.CreationTimestamp;

import ar.com.vidriera.comercio.Comercio;

/**
 * Usuario que entra a un panel. No usa {@code @TenantId} a propósito: el login busca por email
 * antes de saber a qué comercio pertenece la persona. El comercio del usuario logueado es el que
 * después se fija como tenant del panel.
 */
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt de la contraseña. La contraseña en texto plano nunca se guarda. */
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comercio_id")
    private Comercio comercio;

    private boolean activo = true;

    private boolean debeCambiarPassword;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant creadoEn;

    protected Usuario() {
    }

    private Usuario(String email, String passwordHash, String nombre, Rol rol, Comercio comercio) {
        this.email = normalizarEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.rol = rol;
        this.comercio = comercio;
    }

    public static Usuario superAdmin(String email, String passwordHash, String nombre) {
        return new Usuario(email, passwordHash, nombre, Rol.SUPER_ADMIN, null);
    }

    public static Usuario duenoDe(Comercio comercio, String email, String passwordHash, String nombre) {
        Objects.requireNonNull(comercio, "comercio");
        return new Usuario(email, passwordHash, nombre, Rol.DUENO, comercio);
    }

    public static String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = Objects.requireNonNull(nuevoHash);
        this.debeCambiarPassword = false;
    }

    public void exigirCambioDePassword() {
        this.debeCambiarPassword = true;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public Rol getRol() {
        return rol;
    }

    public Comercio getComercio() {
        return comercio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }
}
