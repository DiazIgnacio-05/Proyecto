package ar.com.vidriera.catalogo;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lógica del buscador del catálogo, separada para poder testearla sola. Un producto coincide si
 * contiene TODAS las palabras buscadas (en nombre, descripción o categoría), sin importar
 * mayúsculas ni tildes: "pava electrica" encuentra "Pava eléctrica".
 *
 * <p>Busca en memoria: un comercio chico tiene decenas o pocos cientos de productos, y así no
 * hace falta una búsqueda en la base (que además tendría que respetar el filtro de tenant).
 */
public final class BuscadorCatalogo {

    static final int LARGO_MAXIMO = 80;

    private BuscadorCatalogo() {
    }

    /** Las palabras de la búsqueda, normalizadas. Vacía si no se buscó nada. */
    public static List<String> palabras(String busqueda) {
        if (busqueda == null) {
            return List.of();
        }
        String recortada = busqueda.length() > LARGO_MAXIMO ? busqueda.substring(0, LARGO_MAXIMO) : busqueda;
        String normalizada = normalizar(recortada);
        return normalizada.isEmpty() ? List.of() : Arrays.asList(normalizada.split(" "));
    }

    public static boolean coincide(List<String> palabras, String... textos) {
        if (palabras.isEmpty()) {
            return true;
        }
        StringBuilder todo = new StringBuilder();
        for (String texto : textos) {
            if (texto != null) {
                todo.append(' ').append(normalizar(texto));
            }
        }
        String donde = todo.toString();
        return palabras.stream().allMatch(donde::contains);
    }

    /** Minúsculas, sin tildes ni signos, con un solo espacio entre palabras: "Pava  Eléctrica!" → "pava electrica". */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ñ ]", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }
}
