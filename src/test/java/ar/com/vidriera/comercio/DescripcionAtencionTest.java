package ar.com.vidriera.comercio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class DescripcionAtencionTest {

    // Martes a sábado de 12 a 15:30 y de 20:30 a 00:30.
    private final CalendarioAtencion calendario = new CalendarioAtencion(
            Stream.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                            DayOfWeek.SATURDAY)
                    .flatMap(dia -> Stream.of(
                            new FranjaHoraria(dia, LocalTime.of(12, 0), LocalTime.of(15, 30)),
                            new FranjaHoraria(dia, LocalTime.of(20, 30), LocalTime.of(0, 30))))
                    .toList());

    /** 27/10/2026 es martes. */
    private static LocalDateTime martes(int hora, int minuto) {
        return LocalDateTime.of(2026, 10, 27, hora, minuto);
    }

    private String texto(LocalDateTime momento) {
        return DescripcionAtencion.estado(calendario, momento).orElseThrow().texto();
    }

    @Test
    void abiertoIndicaLaHoraDeCierre() {
        assertThat(texto(martes(13, 0))).isEqualTo("Abierto ahora · cierra a las 15:30");
        assertThat(DescripcionAtencion.estado(calendario, martes(13, 0)).orElseThrow().abierto()).isTrue();
    }

    @Test
    void enLaSiestaAbreHoyALaNoche() {
        assertThat(texto(martes(16, 0))).isEqualTo("Cerrado · abre hoy a las 20:30");
    }

    @Test
    void despuesDelCierreNocturnoAbreAlMediodia() {
        // Miércoles 01:00: ya cerró el turno del martes.
        assertThat(texto(martes(1, 0).plusDays(1))).isEqualTo("Cerrado · abre hoy a las 12:00");
        // Domingo 01:00: terminó el turno del sábado; domingo y lunes cerrado.
        assertThat(texto(martes(1, 0).plusDays(5))).isEqualTo("Cerrado · abre el martes a las 12:00");
    }

    @Test
    void elFinDeSemanaYElLunesEstaCerrado() {
        // Domingo 10:00: cerrado domingo y lunes.
        assertThat(texto(martes(10, 0).plusDays(5))).isEqualTo("Cerrado · abre el martes a las 12:00");
        // Lunes 10:00 → abre "mañana".
        assertThat(texto(martes(10, 0).minusDays(1))).isEqualTo("Cerrado · abre mañana a las 12:00");
    }

    @Test
    void sinHorariosNoHayCartel() {
        assertThat(DescripcionAtencion.estado(new CalendarioAtencion(List.of()), martes(12, 0))).isEmpty();
    }

    @Test
    void laSemanaMuestraLosTurnosYMarcaElDiaDeHoy() {
        List<DescripcionAtencion.Dia> semana = DescripcionAtencion.semana(calendario, DayOfWeek.TUESDAY);
        assertThat(semana).hasSize(7);
        assertThat(semana.getFirst()).isEqualTo(new DescripcionAtencion.Dia("Lunes", "Cerrado", false));
        assertThat(semana.get(1)).isEqualTo(
                new DescripcionAtencion.Dia("Martes", "12:00 a 15:30 y 20:30 a 00:30", true));
        assertThat(semana.getLast().nombre()).isEqualTo("Domingo");
    }
}
