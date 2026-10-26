package ar.com.vidriera.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ar.com.vidriera.comercio.FranjaHoraria;

class HorarioAdminServiceTest {

    @Test
    void convierteLosTurnosCargadosEnFranjas() {
        HorariosForm form = HorariosForm.semanaVacia();
        HorariosForm.Dia martes = form.getDias().get(1);
        martes.setApertura1(LocalTime.of(12, 0));
        martes.setCierre1(LocalTime.of(15, 30));
        martes.setApertura2(LocalTime.of(20, 30));
        martes.setCierre2(LocalTime.of(0, 30));

        assertThat(HorarioAdminService.validar(form)).containsExactly(
                new FranjaHoraria(DayOfWeek.TUESDAY, LocalTime.of(12, 0), LocalTime.of(15, 30)),
                new FranjaHoraria(DayOfWeek.TUESDAY, LocalTime.of(20, 30), LocalTime.of(0, 30)));
    }

    @Test
    void unaSemanaVaciaEsTodoCerrado() {
        assertThat(HorarioAdminService.validar(HorariosForm.semanaVacia())).isEmpty();
    }

    @Test
    void exigeAperturaYCierreJuntos() {
        HorariosForm form = HorariosForm.semanaVacia();
        form.getDias().get(0).setApertura1(LocalTime.of(9, 0));
        assertThatThrownBy(() -> HorarioAdminService.validar(form))
                .isInstanceOf(OperacionRechazadaException.class)
                .hasMessage("Lunes: completá la hora de apertura y la de cierre");
    }

    @Test
    void rechazaAperturaIgualAlCierre() {
        HorariosForm form = HorariosForm.semanaVacia();
        form.getDias().get(4).setApertura1(LocalTime.NOON);
        form.getDias().get(4).setCierre1(LocalTime.NOON);
        assertThatThrownBy(() -> HorarioAdminService.validar(form))
                .hasMessageContaining("Viernes");
    }
}
