package ar.com.vidriera.usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Frena a quien intenta adivinar contraseñas: después de {@value #INTENTOS_MAXIMOS} intentos
 * fallidos con el mismo email, ese usuario queda bloqueado {@link #BLOQUEO} (aunque después acierte).
 *
 * <p>Escucha los eventos que publica Spring Security en cada login. Guarda los contadores en
 * memoria: alcanza para un solo servidor y se reinician al reiniciar la aplicación.
 */
@Component
public class LimitadorIntentosLogin {

    static final int INTENTOS_MAXIMOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Intentos(int cantidad, Instant ultimo) {
    }

    private final Map<String, Intentos> fallidos = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimitadorIntentosLogin(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean estaBloqueado(String email) {
        Intentos intentos = fallidos.get(clave(email));
        if (intentos == null) {
            return false;
        }
        if (intentos.ultimo().plus(BLOQUEO).isBefore(reloj.instant())) {
            fallidos.remove(clave(email));
            return false;
        }
        return intentos.cantidad() >= INTENTOS_MAXIMOS;
    }

    public void registrarFallo(String email) {
        Instant ahora = reloj.instant();
        fallidos.merge(clave(email), new Intentos(1, ahora), (anterior, nuevo) ->
                anterior.ultimo().plus(BLOQUEO).isBefore(ahora)
                        ? nuevo
                        : new Intentos(anterior.cantidad() + 1, ahora));
    }

    public void registrarExito(String email) {
        fallidos.remove(clave(email));
    }

    @EventListener
    void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        registrarFallo(evento.getAuthentication().getName());
    }

    @EventListener
    void alEntrar(AuthenticationSuccessEvent evento) {
        registrarExito(evento.getAuthentication().getName());
    }

    private static String clave(String email) {
        return email == null ? "" : email.strip().toLowerCase();
    }
}
