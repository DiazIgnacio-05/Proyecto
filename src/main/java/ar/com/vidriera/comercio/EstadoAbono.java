package ar.com.vidriera.comercio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Situación del abono mensual de un comercio en una fecha dada, para el panel de super-admin:
 * "Vence en 5 días", "Vencido hace 3 días"...
 *
 * @param nivel para el color del cartel
 * @param dias  días hasta el vencimiento (negativo si ya venció); null si no hay fecha cargada
 */
public record EstadoAbono(Nivel nivel, Long dias, String texto) {

    /** A partir de cuántos días antes del vencimiento se avisa. */
    static final int DIAS_DE_AVISO = 5;

    public enum Nivel {
        AL_DIA, VENCE_PRONTO, VENCIDO, SIN_FECHA
    }

    public static EstadoAbono de(LocalDate vencimiento, LocalDate hoy) {
        if (vencimiento == null) {
            return new EstadoAbono(Nivel.SIN_FECHA, null, "Sin fecha de vencimiento");
        }
        long dias = ChronoUnit.DAYS.between(hoy, vencimiento);
        if (dias < 0) {
            return new EstadoAbono(Nivel.VENCIDO, dias, "Vencido hace " + plural(-dias));
        }
        if (dias == 0) {
            return new EstadoAbono(Nivel.VENCE_PRONTO, 0L, "Vence hoy");
        }
        Nivel nivel = dias <= DIAS_DE_AVISO ? Nivel.VENCE_PRONTO : Nivel.AL_DIA;
        return new EstadoAbono(nivel, dias, "Vence en " + plural(dias));
    }

    /**
     * Nuevo vencimiento al registrar el pago de un mes: se suma a partir del vencimiento actual, o de
     * hoy si ya estaba vencido (no se cobran los días en que estuvo sin pagar).
     */
    public static LocalDate despuesDePagarUnMes(LocalDate vencimiento, LocalDate hoy) {
        LocalDate desde = vencimiento == null || vencimiento.isBefore(hoy) ? hoy : vencimiento;
        return desde.plusMonths(1);
    }

    public boolean vencido() {
        return nivel == Nivel.VENCIDO;
    }

    private static String plural(long dias) {
        return dias == 1 ? "1 día" : dias + " días";
    }
}
