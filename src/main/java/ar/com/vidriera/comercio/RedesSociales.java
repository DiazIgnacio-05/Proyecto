package ar.com.vidriera.comercio;

import jakarta.persistence.Embeddable;

/** Usuarios o links de redes sociales del comercio (todos opcionales). */
@Embeddable
public class RedesSociales {

    private String instagram;
    private String facebook;
    private String tiktok;

    public boolean tieneAlguna() {
        return instagram != null || facebook != null || tiktok != null;
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
