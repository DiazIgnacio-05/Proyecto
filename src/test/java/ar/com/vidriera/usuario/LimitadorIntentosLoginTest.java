package ar.com.vidriera.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class LimitadorIntentosLoginTest {

    /** Reloj que avanza solo cuando el test lo pide. */
    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-10-27T15:00:00Z");

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }

    private final RelojManual reloj = new RelojManual();
    private final LimitadorIntentosLogin limitador = new LimitadorIntentosLogin(reloj);

    @Test
    void bloqueaDespuesDeCincoFallos() {
        for (int i = 0; i < LimitadorIntentosLogin.INTENTOS_MAXIMOS - 1; i++) {
            limitador.registrarFallo("ana@test.com");
        }
        assertThat(limitador.estaBloqueado("ana@test.com")).isFalse();
        limitador.registrarFallo("ANA@test.com ");   // mismo email, escrito distinto
        assertThat(limitador.estaBloqueado("ana@test.com")).isTrue();
        assertThat(limitador.estaBloqueado("otro@test.com")).isFalse();
    }

    @Test
    void elBloqueoVenceALos15Minutos() {
        for (int i = 0; i < LimitadorIntentosLogin.INTENTOS_MAXIMOS; i++) {
            limitador.registrarFallo("ana@test.com");
        }
        reloj.avanzar(Duration.ofMinutes(14));
        assertThat(limitador.estaBloqueado("ana@test.com")).isTrue();
        reloj.avanzar(Duration.ofMinutes(2));
        assertThat(limitador.estaBloqueado("ana@test.com")).isFalse();
    }

    @Test
    void unLoginCorrectoReiniciaElContador() {
        for (int i = 0; i < LimitadorIntentosLogin.INTENTOS_MAXIMOS - 1; i++) {
            limitador.registrarFallo("ana@test.com");
        }
        limitador.registrarExito("ana@test.com");
        limitador.registrarFallo("ana@test.com");
        assertThat(limitador.estaBloqueado("ana@test.com")).isFalse();
    }
}
