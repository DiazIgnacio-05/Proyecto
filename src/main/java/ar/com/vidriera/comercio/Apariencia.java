package ar.com.vidriera.comercio;

import java.util.regex.Pattern;

import jakarta.persistence.Embeddable;

/** Identidad visual del sitio: logo, portada y colores (formato {@code #RRGGBB}). */
@Embeddable
public class Apariencia {

    public static final String COLOR_PRIMARIO_POR_DEFECTO = "#1F6F5C";
    public static final String COLOR_SECUNDARIO_POR_DEFECTO = "#F4B942";

    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private String logoUrl;
    private String portadaUrl;
    private String colorPrimario = COLOR_PRIMARIO_POR_DEFECTO;
    private String colorSecundario = COLOR_SECUNDARIO_POR_DEFECTO;

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPortadaUrl() {
        return portadaUrl;
    }

    public void setPortadaUrl(String portadaUrl) {
        this.portadaUrl = portadaUrl;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = validarColor(colorPrimario);
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = validarColor(colorSecundario);
    }

    private static String validarColor(String color) {
        if (color == null || !COLOR.matcher(color).matches()) {
            throw new IllegalArgumentException("Color inválido: '" + color + "'. Formato esperado: #RRGGBB");
        }
        return color.toUpperCase();
    }
}
