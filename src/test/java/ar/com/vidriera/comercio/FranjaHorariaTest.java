package ar.com.vidriera.comercio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class FranjaHorariaTest {

    // 2026-10-27 es martes.
    private static final LocalDateTime MARTES_MEDIODIA = LocalDateTime.of(2026, 10, 27, 12, 0);

    private final FranjaHoraria mediodiaMartes =
            new FranjaHoraria(DayOfWeek.TUESDAY, LocalTime.of(12, 0), LocalTime.of(15, 30));
    private final FranjaHoraria nocheMartes =
            new FranjaHoraria(DayOfWeek.TUESDAY, LocalTime.of(20, 30), LocalTime.of(0, 30));

    @Test
    void incluyeLaAperturaPeroNoElCierre() {
        assertThat(mediodiaMartes.incluye(MARTES_MEDIODIA)).isTrue();
        assertThat(mediodiaMartes.incluye(MARTES_MEDIODIA.withHour(15).withMinute(29))).isTrue();
        assertThat(mediodiaMartes.incluye(MARTES_MEDIODIA.withHour(15).withMinute(30))).isFalse();
        assertThat(mediodiaMartes.incluye(MARTES_MEDIODIA.withHour(11).withMinute(59))).isFalse();
    }

    @Test
    void noIncluyeElMismoHorarioOtroDia() {
        assertThat(mediodiaMartes.incluye(MARTES_MEDIODIA.plusDays(1))).isFalse();
    }

    @Test
    void franjaQueCruzaLaMedianocheSigueAbiertaElDiaSiguiente() {
        assertThat(nocheMartes.cruzaMedianoche()).isTrue();
        assertThat(nocheMartes.incluye(MARTES_MEDIODIA.withHour(23))).isTrue();
        // Miércoles 00:15: todavía es el turno del martes.
        assertThat(nocheMartes.incluye(MARTES_MEDIODIA.plusDays(1).withHour(0).withMinute(15))).isTrue();
        assertThat(nocheMartes.incluye(MARTES_MEDIODIA.plusDays(1).withHour(0).withMinute(30))).isFalse();
        // Martes 00:15 corresponde al turno del lunes, que no existe.
        assertThat(nocheMartes.incluye(MARTES_MEDIODIA.withHour(0).withMinute(15))).isFalse();
    }

    @Test
    void elDomingoQueCruzaLaMedianocheTerminaElLunes() {
        FranjaHoraria domingo = new FranjaHoraria(DayOfWeek.SUNDAY, LocalTime.of(21, 0), LocalTime.of(1, 0));
        LocalDateTime lunesMedianoche = LocalDateTime.of(2026, 11, 2, 0, 30);
        assertThat(lunesMedianoche.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(domingo.incluye(lunesMedianoche)).isTrue();
    }

    @Test
    void cierreAMedianocheSeTomaComoFinDelDia() {
        FranjaHoraria hastaMedianoche =
                new FranjaHoraria(DayOfWeek.FRIDAY, LocalTime.of(20, 0), LocalTime.MIDNIGHT);
        LocalDateTime viernes = LocalDateTime.of(2026, 10, 30, 23, 59);
        assertThat(hastaMedianoche.incluye(viernes)).isTrue();
        assertThat(hastaMedianoche.incluye(viernes.plusMinutes(1))).isFalse();
    }

    @Test
    void rechazaAperturaIgualAlCierre() {
        assertThatThrownBy(() -> new FranjaHoraria(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
