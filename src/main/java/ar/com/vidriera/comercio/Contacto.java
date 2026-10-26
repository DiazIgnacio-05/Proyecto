package ar.com.vidriera.comercio;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;

/**
 * Datos de contacto y ubicación. Es un {@code @Embeddable}: una clase Java propia cuyas columnas
 * viven en la tabla {@code comercio} (no tiene tabla ni id propios).
 */
@Embeddable
public class Contacto {

    /** Número de WhatsApp tal como lo carga el comercio; se normaliza al armar el link. */
    private String whatsapp;
    private String telefono;
    private String email;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;

    protected Contacto() {
    }

    public Contacto(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
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
}
