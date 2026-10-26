package ar.com.vidriera.admin;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del comercio que puede editar el dueño. El slug (subdominio), el rubro y el estado no
 * están acá: los maneja el super-admin.
 */
public class ComercioForm {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre es demasiado largo (máx. 120)")
    private String nombre;

    @Size(max = 500, message = "La descripción es demasiado larga (máx. 500)")
    private String descripcion;

    @NotBlank(message = "El WhatsApp es obligatorio: ahí llegan los pedidos")
    @Size(max = 30, message = "El WhatsApp es demasiado largo")
    private String whatsapp;

    @Size(max = 30, message = "El teléfono es demasiado largo")
    private String telefono;

    @Email(message = "El email no es válido")
    @Size(max = 120, message = "El email es demasiado largo")
    private String email;

    @Size(max = 200, message = "La dirección es demasiado larga")
    private String direccion;

    @DecimalMin(value = "-90", message = "Latitud inválida")
    @DecimalMax(value = "90", message = "Latitud inválida")
    private BigDecimal latitud;

    @DecimalMin(value = "-180", message = "Longitud inválida")
    @DecimalMax(value = "180", message = "Longitud inválida")
    private BigDecimal longitud;

    @Size(max = 100, message = "Instagram: demasiado largo")
    private String instagram;

    @Size(max = 100, message = "Facebook: demasiado largo")
    private String facebook;

    @Size(max = 100, message = "TikTok: demasiado largo")
    private String tiktok;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color inválido")
    private String colorPrimario;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color inválido")
    private String colorSecundario;

    @Size(max = 70, message = "El título para Google es demasiado largo (máx. 70)")
    private String seoTitulo;

    @Size(max = 160, message = "La descripción para Google es demasiado larga (máx. 160)")
    private String seoDescripcion;

    private boolean ofreceEnvio;

    @Min(value = 0, message = "La cantidad de mesas no puede ser negativa")
    @Max(value = 200, message = "Máximo 200 mesas")
    private int cantidadMesas;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public String getTiktok() {
        return tiktok;
    }

    public void setTiktok(String tiktok) {
        this.tiktok = tiktok;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public String getSeoTitulo() {
        return seoTitulo;
    }

    public void setSeoTitulo(String seoTitulo) {
        this.seoTitulo = seoTitulo;
    }

    public String getSeoDescripcion() {
        return seoDescripcion;
    }

    public void setSeoDescripcion(String seoDescripcion) {
        this.seoDescripcion = seoDescripcion;
    }

    public boolean isOfreceEnvio() {
        return ofreceEnvio;
    }

    public void setOfreceEnvio(boolean ofreceEnvio) {
        this.ofreceEnvio = ofreceEnvio;
    }

    public int getCantidadMesas() {
        return cantidadMesas;
    }

    public void setCantidadMesas(int cantidadMesas) {
        this.cantidadMesas = cantidadMesas;
    }
}
