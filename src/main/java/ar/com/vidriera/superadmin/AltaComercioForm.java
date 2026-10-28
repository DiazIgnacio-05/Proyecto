package ar.com.vidriera.superadmin;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import ar.com.vidriera.comercio.Rubro;

/** Datos mínimos para dar de alta un comercio; el resto lo completa el dueño desde su panel. */
public class AltaComercioForm {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre es demasiado largo")
    private String nombre;

    /** Opcional: si queda vacío se sugiere a partir del nombre. */
    @Size(max = 63, message = "El subdominio es demasiado largo (máx. 63)")
    private String slug;

    @NotNull(message = "Elegí el rubro")
    private Rubro rubro;

    @NotBlank(message = "El WhatsApp es obligatorio")
    @Size(max = 30, message = "El WhatsApp es demasiado largo")
    private String whatsapp;

    @NotBlank(message = "El nombre del dueño es obligatorio")
    @Size(max = 120, message = "El nombre del dueño es demasiado largo")
    private String nombreDueno;

    @NotBlank(message = "El email del dueño es obligatorio")
    @Email(message = "El email del dueño no es válido")
    @Size(max = 120, message = "El email es demasiado largo")
    private String emailDueno;

    @NotNull(message = "Indicá hasta cuándo está pago el abono")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate vencimientoAbono;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Rubro getRubro() {
        return rubro;
    }

    public void setRubro(Rubro rubro) {
        this.rubro = rubro;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getNombreDueno() {
        return nombreDueno;
    }

    public void setNombreDueno(String nombreDueno) {
        this.nombreDueno = nombreDueno;
    }

    public String getEmailDueno() {
        return emailDueno;
    }

    public void setEmailDueno(String emailDueno) {
        this.emailDueno = emailDueno;
    }

    public LocalDate getVencimientoAbono() {
        return vencimientoAbono;
    }

    public void setVencimientoAbono(LocalDate vencimientoAbono) {
        this.vencimientoAbono = vencimientoAbono;
    }
}
