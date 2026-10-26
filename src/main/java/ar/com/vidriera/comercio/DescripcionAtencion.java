package ar.com.vidriera.comercio;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Convierte los horarios en los textos que ve el cliente: "Abierto ahora · cierra a las 15:30". */
public final class DescripcionAtencion {

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Estado actual del comercio. {@code abierto} se usa para el color del cartel. */
    public record Estado(boolean abierto, String texto) {
    }

    /** Un renglón de la tabla de horarios semanal. */
    public record Dia(String nombre, String horario, boolean hoy) {
    }

    private DescripcionAtencion() {
    }

    /** Vacío si el comercio no cargó horarios (no se muestra ningún cartel). */
    public static Optional<Estado> estado(CalendarioAtencion calendario, LocalDateTime ahora) {
        if (!calendario.tieneHorarios()) {
            return Optional.empty();
        }
        Optional<LocalTime> cierre = calendario.cierreActual(ahora);
        if (cierre.isPresent()) {
            return Optional.of(new Estado(true, "Abierto ahora · cierra a las " + HORA.format(cierre.get())));
        }
        return Optional.of(new Estado(false, calendario.proximaApertura(ahora)
                .map(apertura -> "Cerrado · abre " + cuando(ahora, apertura) + " a las " + HORA.format(apertura))
                .orElse("Cerrado")));
    }

    public static List<Dia> semana(CalendarioAtencion calendario, DayOfWeek hoy) {
        return Arrays.stream(DayOfWeek.values())
                .map(dia -> new Dia(nombre(dia), horario(calendario.franjasDelDia(dia)), dia == hoy))
                .toList();
    }

    /** "Lunes", "Martes"... */
    public static String nombre(DayOfWeek dia) {
        String nombre = dia.getDisplayName(TextStyle.FULL, ES_AR);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }

    private static String horario(List<FranjaHoraria> franjas) {
        if (franjas.isEmpty()) {
            return "Cerrado";
        }
        return franjas.stream()
                .map(f -> HORA.format(f.apertura()) + " a " + HORA.format(f.cierre()))
                .collect(Collectors.joining(" y "));
    }

    private static String cuando(LocalDateTime ahora, LocalDateTime apertura) {
        long dias = ChronoUnit.DAYS.between(ahora.toLocalDate(), apertura.toLocalDate());
        if (dias == 0) {
            return "hoy";
        }
        if (dias == 1) {
            return "mañana";
        }
        return "el " + apertura.getDayOfWeek().getDisplayName(TextStyle.FULL, ES_AR);
    }
}
