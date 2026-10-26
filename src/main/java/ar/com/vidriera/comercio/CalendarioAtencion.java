package ar.com.vidriera.comercio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Responde preguntas sobre los horarios de un comercio: si está abierto, cuándo abre, etc. */
public class CalendarioAtencion {

    private final List<FranjaHoraria> franjas;

    public CalendarioAtencion(List<FranjaHoraria> franjas) {
        this.franjas = franjas.stream()
                .sorted(Comparator.comparing(FranjaHoraria::dia).thenComparing(FranjaHoraria::apertura))
                .toList();
    }

    public boolean estaAbierto(LocalDateTime ahora) {
        return franjas.stream().anyMatch(franja -> franja.incluye(ahora));
    }

    /** Próximo momento de apertura estrictamente posterior a {@code ahora}, si hay horarios cargados. */
    public Optional<LocalDateTime> proximaApertura(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        for (int dias = 0; dias <= 7; dias++) {
            LocalDate fecha = hoy.plusDays(dias);
            Optional<LocalDateTime> apertura = franjasDelDia(fecha.getDayOfWeek()).stream()
                    .map(franja -> fecha.atTime(franja.apertura()))
                    .filter(momento -> momento.isAfter(ahora))
                    .findFirst();
            if (apertura.isPresent()) {
                return apertura;
            }
        }
        return Optional.empty();
    }

    /** Hora de cierre del turno en curso, si el comercio está abierto en este momento. */
    public Optional<LocalTime> cierreActual(LocalDateTime ahora) {
        return franjas.stream().filter(franja -> franja.incluye(ahora)).map(FranjaHoraria::cierre).findFirst();
    }

    public List<FranjaHoraria> franjasDelDia(DayOfWeek dia) {
        return franjas.stream().filter(franja -> franja.dia() == dia).toList();
    }

    /** Franjas agrupadas por día, de lunes a domingo (los días sin atención quedan con lista vacía). */
    public Map<DayOfWeek, List<FranjaHoraria>> resumenSemanal() {
        Map<DayOfWeek, List<FranjaHoraria>> resumen = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dia : DayOfWeek.values()) {
            resumen.put(dia, franjasDelDia(dia));
        }
        return resumen;
    }

    public boolean tieneHorarios() {
        return !franjas.isEmpty();
    }
}
