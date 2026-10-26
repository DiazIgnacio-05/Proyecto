package ar.com.vidriera.imagen;

/** Tamaños que se generan de cada foto. El celular baja solo el que necesita. */
public enum TamanoImagen {

    /** Miniatura para los listados (ancho en píxeles). */
    CHICA(480),
    /** Detalle y portada. */
    GRANDE(1200);

    private final int ancho;

    TamanoImagen(int ancho) {
        this.ancho = ancho;
    }

    public int getAncho() {
        return ancho;
    }

    /** Sufijo del archivo: {@code -480.jpg}. */
    String sufijo() {
        return "-" + ancho + ".jpg";
    }
}
