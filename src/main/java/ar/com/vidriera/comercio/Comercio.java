package ar.com.vidriera.comercio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Un cliente de la plataforma (el "tenant"). No extiende {@code EntidadDelComercio}: es la entidad
 * que define a los tenants, y el super-admin y el resolvedor de hosts necesitan verlos a todos.
 */
@Entity
public class Comercio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = Slug.LARGO_MAXIMO, updatable = false)
    private String slug;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rubro rubro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoComercio estado;

    private String descripcion;

    @Embedded
    private Contacto contacto;

    @Embedded
    private Apariencia apariencia;

    @Embedded
    private RedesSociales redes;

    @Embedded
    private Seo seo;

    private String dominioPropio;

    private int cantidadMesas;

    /** Si el pedido por WhatsApp ofrece "envío a domicilio" además de retiro en el local. */
    private boolean ofreceEnvio;

    @Column(nullable = false)
    private LocalDate fechaAlta;

    private LocalDate vencimientoAbono;

    private String notasInternas;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant creadoEn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "comercio_modulo", joinColumns = @JoinColumn(name = "comercio_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "modulo", nullable = false)
    private Set<Modulo> modulos = new HashSet<>();

    /** Requerido por JPA. */
    protected Comercio() {
    }

    public Comercio(String slug, String nombre, Rubro rubro, String whatsapp, LocalDate fechaAlta) {
        this.slug = Slug.validar(slug);
        this.nombre = requerido(nombre, "nombre");
        this.rubro = Objects.requireNonNull(rubro, "rubro");
        this.contacto = new Contacto(requerido(whatsapp, "whatsapp"));
        this.fechaAlta = Objects.requireNonNull(fechaAlta, "fechaAlta");
        this.estado = EstadoComercio.ACTIVO;
        this.apariencia = new Apariencia();
    }

    public void suspender() {
        if (estado == EstadoComercio.BAJA) {
            throw new IllegalStateException("No se puede suspender un comercio dado de baja");
        }
        estado = EstadoComercio.SUSPENDIDO;
    }

    public void reactivar() {
        estado = EstadoComercio.ACTIVO;
    }

    public void darDeBaja() {
        estado = EstadoComercio.BAJA;
    }

    public boolean tieneModulo(Modulo modulo) {
        return modulos.contains(modulo);
    }

    public void activarModulo(Modulo modulo) {
        modulos.add(Objects.requireNonNull(modulo));
    }

    public void desactivarModulo(Modulo modulo) {
        modulos.remove(modulo);
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = requerido(nombre, "nombre");
    }

    public Rubro getRubro() {
        return rubro;
    }

    public EstadoComercio getEstado() {
        return estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Contacto getContacto() {
        return contacto;
    }

    public Apariencia getApariencia() {
        return apariencia;
    }

    // Hibernate deja en null un @Embedded cuyas columnas son todas null; estos getters lo evitan.
    public RedesSociales getRedes() {
        if (redes == null) {
            redes = new RedesSociales();
        }
        return redes;
    }

    public Seo getSeo() {
        if (seo == null) {
            seo = new Seo();
        }
        return seo;
    }

    public String getDominioPropio() {
        return dominioPropio;
    }

    public void setDominioPropio(String dominioPropio) {
        this.dominioPropio = dominioPropio == null ? null : dominioPropio.trim().toLowerCase();
    }

    public int getCantidadMesas() {
        return cantidadMesas;
    }

    public void setCantidadMesas(int cantidadMesas) {
        if (cantidadMesas < 0 || cantidadMesas > 200) {
            throw new IllegalArgumentException("La cantidad de mesas debe estar entre 0 y 200");
        }
        this.cantidadMesas = cantidadMesas;
    }

    /** Las mesas solo cuentan si el rubro trabaja con mesas (restaurantes). */
    public boolean tieneMesa(int numero) {
        return rubro.usaMesas() && numero >= 1 && numero <= cantidadMesas;
    }

    public boolean isOfreceEnvio() {
        return ofreceEnvio;
    }

    public void setOfreceEnvio(boolean ofreceEnvio) {
        this.ofreceEnvio = ofreceEnvio;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public LocalDate getVencimientoAbono() {
        return vencimientoAbono;
    }

    public void setVencimientoAbono(LocalDate vencimientoAbono) {
        this.vencimientoAbono = vencimientoAbono;
    }

    public String getNotasInternas() {
        return notasInternas;
    }

    public void setNotasInternas(String notasInternas) {
        this.notasInternas = notasInternas;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Set<Modulo> getModulos() {
        return Set.copyOf(modulos);
    }

    private static String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }
}
