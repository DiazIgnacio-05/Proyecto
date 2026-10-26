package ar.com.vidriera.comercio;

public enum EstadoComercio {

    /** Abono al día: sitio visible y panel habilitado. */
    ACTIVO,
    /** Abono impago: el sitio muestra "no disponible" y el panel queda en solo lectura. */
    SUSPENDIDO,
    /** Cliente dado de baja: datos conservados, sin sitio ni panel. */
    BAJA;

    public boolean sitioPublicoVisible() {
        return this == ACTIVO;
    }

    public boolean permiteIngresoAlPanel() {
        return this != BAJA;
    }

    public boolean permiteEditar() {
        return this == ACTIVO;
    }
}
