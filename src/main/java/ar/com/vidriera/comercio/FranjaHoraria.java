package ar.com.vidriera.comercio;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Un turno de atención en un día de la semana, por ejemplo "martes de 20:30 a 00:30".
 *
 * <p>Es un {@code record}: una clase inmutable cuyo constructor, getters, {@code equals} y
 * {@code toString} genera el compilador. Si el cierre es anterior a la apertura, la franja
 * cruza la medianoche y termina al día siguiente.
 */
public record FranjaHoraria(DayOfWeek dia, LocalTime apertura, LocalTime cierre) {

    public FranjaHoraria {
        Objects.requireNonNull(dia, "dia");
        Objects.requireNonNull(apertura, "apertura");
        Objects.requireNonNull(cierre, "cierre");
        if (apertura.equals(cierre)) {
            throw new IllegalArgumentException("La apertura y el cierre no pueden ser iguales");
        }
    }

    public boolean cruzaMedianoche() {
        return cierre.isBefore(apertura);
    }

    /** Indica si el momento dado (en hora local del comercio) cae dentro de esta franja. */
    public boolean incluye(LocalDateTime momento) {
        DayOfWeek diaMomento = momento.getDayOfWeek();
        LocalTime hora = momento.toLocalTime();
        if (!cruzaMedianoche()) {
            return diaMomento == dia && !hora.isBefore(apertura) && hora.isBefore(cierre);
        }
        boolean tramoAntesDeMedianoche = diaMomento == dia && !hora.isBefore(apertura);
        boolean tramoDespuesDeMedianoche = diaMomento == dia.plus(1) && hora.isBefore(cierre);
        return tramoAntesDeMedianoche || tramoDespuesDeMedianoche;
    }
}
