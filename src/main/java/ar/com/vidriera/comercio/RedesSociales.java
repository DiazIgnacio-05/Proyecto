package ar.com.vidriera.comercio;

import java.util.regex.Pattern;

import jakarta.persistence.Embeddable;

/**
 * Redes sociales del comercio (todas opcionales). Se puede cargar el usuario ("@nogal") o el link
 * completo; las URLs que se muestran en el sitio se arman acá.
 */
@Embeddable
public class RedesSociales {

    private static final Pattern USUARIO = Pattern.compile("^[A-Za-z0-9._-]{1,60}$");

    private String instagram;
    private String facebook;
    private String tiktok;

    public boolean tieneAlguna() {
        return urlInstagram() != null || urlFacebook() != null || urlTiktok() != null;
    }

    public String urlInstagram() {
        return url(instagram, "https://www.instagram.com/");
    }

    public String urlFacebook() {
        return url(facebook, "https://www.facebook.com/");
    }

    public String urlTiktok() {
        return url(tiktok, "https://www.tiktok.com/@");
    }

    /**
     * Solo acepta links https o nombres de usuario simples: así un valor como
     * {@code javascript:...} nunca termina en un {@code href}.
     */
    static String url(String valor, String prefijo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String v = valor.strip();
        if (v.startsWith("https://")) {
            return v;
        }
        if (v.startsWith("@")) {
            v = v.substring(1);
        }
        return USUARIO.matcher(v).matches() ? prefijo + v : null;
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
}
