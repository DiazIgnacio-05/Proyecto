package ar.com.vidriera.comercio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reglas del slug: el identificador del comercio que se usa como subdominio
 * ({@code <slug>.vidriera.com.ar}). Solo minúsculas, números y guiones, sin guion al inicio ni al final.
 */
public final class Slug {

    public static final int LARGO_MAXIMO = 63;

    private static final Pattern FORMATO = Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");

    /** Subdominios que usa la plataforma y no pueden asignarse a un comercio. */
    private static final Set<String> RESERVADOS =
            Set.of("www", "app", "admin", "api", "panel", "mail", "static", "cdn", "soporte", "demo");

    private Slug() {
    }

    public static boolean esValido(String slug) {
        return slug != null && slug.length() <= LARGO_MAXIMO && FORMATO.matcher(slug).matches();
    }

    public static boolean esReservado(String slug) {
        return RESERVADOS.contains(slug);
    }

    /** Propone un slug a partir del nombre: "Pizzería Don José" → "pizzeria-don-jose". */
    public static String sugerir(String nombre) {
        if (nombre == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.length() > LARGO_MAXIMO) {
            slug = slug.substring(0, LARGO_MAXIMO).replaceAll("-+$", "");
        }
        return slug;
    }

    public static String validar(String slug) {
        if (!esValido(slug)) {
            throw new IllegalArgumentException(
                    "Slug inválido: '" + slug + "'. Usá minúsculas, números y guiones (máx. 63).");
        }
        if (esReservado(slug)) {
            throw new IllegalArgumentException("El slug '" + slug + "' está reservado por la plataforma");
        }
        return slug;
    }
}
