package ar.com.vidriera.usuario;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Genera contraseñas temporales para el alta de un comercio o el reseteo de clave. Usa
 * {@link SecureRandom} (aleatoriedad apta para seguridad, a diferencia de {@code Random}) y un
 * alfabeto sin caracteres que se confunden al dictarlos o leerlos (0/O, 1/l/I).
 * Formato: tres bloques de cuatro, ej. {@code kq7m-x3pf-9tzr} (~62 bits de azar).
 */
@Component
public class GeneradorContrasenas {

    private static final String ALFABETO = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final int BLOQUES = 3;
    private static final int LARGO_BLOQUE = 4;

    private final SecureRandom azar = new SecureRandom();

    public String generar() {
        StringBuilder clave = new StringBuilder();
        for (int bloque = 0; bloque < BLOQUES; bloque++) {
            if (bloque > 0) {
                clave.append('-');
            }
            for (int i = 0; i < LARGO_BLOQUE; i++) {
                clave.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
            }
        }
        return clave.toString();
    }
}
