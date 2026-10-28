package ar.com.vidriera.comercio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class EstadoAbonoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 28);

    @Test
    void alDiaCuandoFaltaMasDeCincoDias() {
        EstadoAbono estado = EstadoAbono.de(HOY.plusDays(20), HOY);
        assertThat(estado.nivel()).isEqualTo(EstadoAbono.Nivel.AL_DIA);
        assertThat(estado.texto()).isEqualTo("Vence en 20 días");
    }

    @Test
    void avisaCuandoVencePronto() {
        assertThat(EstadoAbono.de(HOY.plusDays(5), HOY).nivel()).isEqualTo(EstadoAbono.Nivel.VENCE_PRONTO);
        assertThat(EstadoAbono.de(HOY.plusDays(1), HOY).texto()).isEqualTo("Vence en 1 día");
        assertThat(EstadoAbono.de(HOY, HOY).texto()).isEqualTo("Vence hoy");
    }

    @Test
    void vencido() {
        EstadoAbono estado = EstadoAbono.de(HOY.minusDays(3), HOY);
        assertThat(estado.vencido()).isTrue();
        assertThat(estado.dias()).isEqualTo(-3);
        assertThat(estado.texto()).isEqualTo("Vencido hace 3 días");
    }

    @Test
    void sinFecha() {
        assertThat(EstadoAbono.de(null, HOY).nivel()).isEqualTo(EstadoAbono.Nivel.SIN_FECHA);
    }

    @Test
    void pagarUnMesSumaDesdeElVencimientoSiTodaviaNoVencio() {
        assertThat(EstadoAbono.despuesDePagarUnMes(LocalDate.of(2026, 11, 10), HOY)).isEqualTo(LocalDate.of(2026, 12, 10));
    }

    @Test
    void pagarUnMesSumaDesdeHoySiYaEstabaVencido() {
        assertThat(EstadoAbono.despuesDePagarUnMes(LocalDate.of(2026, 9, 1), HOY)).isEqualTo(LocalDate.of(2026, 11, 28));
        assertThat(EstadoAbono.despuesDePagarUnMes(null, HOY)).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    @Test
    void elMesQueSigueAUnTreintaYUnoTerminaElUltimoDia() {
        assertThat(EstadoAbono.despuesDePagarUnMes(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 1, 20)))
                .isEqualTo(LocalDate.of(2027, 2, 28));
    }
}
