package ar.com.vidriera.comercio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class CalendarioAtencionTest {

    // Restaurante: martes a domingo de 12 a 15:30 y de 20:30 a 00:30. Lunes cerrado.
    private final CalendarioAtencion restaurante = new CalendarioAtencion(
            List.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).stream()
                    .flatMap(dia -> List.of(
                            new FranjaHoraria(dia, LocalTime.of(20, 30), LocalTime.of(0, 30)),
                            new FranjaHoraria(dia, LocalTime.of(12, 0), LocalTime.of(15, 30))).stream())
                    .toList());

    private static LocalDateTime lunes(int hora, int minuto) {
        return LocalDateTime.of(2026, 10, 26, hora, minuto); // 26/10/2026 es lunes
    }

    @Test
    void estaAbiertoDuranteUnTurno() {
        assertThat(restaurante.estaAbierto(lunes(13, 0).plusDays(1))).isTrue();
    }

    @Test
    void estaCerradoEnLaSiesta() {
        assertThat(restaurante.estaAbierto(lunes(17, 0).plusDays(1))).isFalse();
    }

    @Test
    void elLunesALaMadrugadaSigueAbiertoPorElTurnoDelDomingo() {
        assertThat(restaurante.estaAbierto(lunes(0, 10))).isTrue();
        assertThat(restaurante.estaAbierto(lunes(1, 0))).isFalse();
    }

    @Test
    void proximaAperturaEnLaSiestaEsALaNoche() {
        assertThat(restaurante.proximaApertura(lunes(17, 0).plusDays(1)))
                .contains(lunes(20, 30).plusDays(1));
    }

    @Test
    void proximaAperturaElLunesCerradoEsElMartesAlMediodia() {
        assertThat(restaurante.proximaApertura(lunes(10, 0))).contains(lunes(12, 0).plusDays(1));
    }

    @Test
    void resumenSemanalOrdenaLosTurnosYMarcaLosDiasCerrados() {
        var resumen = restaurante.resumenSemanal();
        assertThat(resumen).hasSize(7);
        assertThat(resumen.get(DayOfWeek.MONDAY)).isEmpty();
        assertThat(resumen.get(DayOfWeek.TUESDAY))
                .extracting(FranjaHoraria::apertura)
                .containsExactly(LocalTime.of(12, 0), LocalTime.of(20, 30));
    }

    @Test
    void sinHorariosNoHayProximaApertura() {
        CalendarioAtencion vacio = new CalendarioAtencion(List.of());
        assertThat(vacio.tieneHorarios()).isFalse();
        assertThat(vacio.estaAbierto(lunes(12, 0))).isFalse();
        assertThat(vacio.proximaApertura(lunes(12, 0))).isEmpty();
    }
}
