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

    /** Color de texto legible sobre el color primario: blanco sobre colores oscuros, casi negro sobre claros. */
    public String getColorSobrePrimario() {
        return textoLegibleSobre(colorPrimario);
    }

    public String getColorSobreSecundario() {
        return textoLegibleSobre(colorSecundario);
    }

    /**
     * Elige el texto con mejor contraste según la luminancia relativa de la WCAG (la norma de
     * accesibilidad web): compara el contraste contra blanco y contra casi negro.
     */
    static String textoLegibleSobre(String colorFondo) {
        double fondo = luminancia(colorFondo);
        double contrasteBlanco = 1.05 / (fondo + 0.05);
        double contrasteOscuro = (fondo + 0.05) / (luminancia(TEXTO_OSCURO) + 0.05);
        return contrasteBlanco >= contrasteOscuro ? TEXTO_CLARO : TEXTO_OSCURO;
    }

    private static final String TEXTO_CLARO = "#FFFFFF";
    private static final String TEXTO_OSCURO = "#1D1D1F";

    private static double luminancia(String color) {
        int rgb = Integer.parseInt(color.substring(1), 16);
        return 0.2126 * canal(rgb >> 16) + 0.7152 * canal(rgb >> 8) + 0.0722 * canal(rgb);
    }

    private static double canal(int valor) {
        double c = (valor & 0xFF) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static String validarColor(String color) {
        if (color == null || !COLOR.matcher(color).matches()) {
            throw new IllegalArgumentException("Color inválido: '" + color + "'. Formato esperado: #RRGGBB");
        }
        return color.toUpperCase();
    }
}
