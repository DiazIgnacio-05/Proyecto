package ar.com.vidriera.comercio;

import java.util.List;

/**
 * Rubro del comercio. Cada rubro declara qué funciones usa; el resto del código pregunta por la
 * función ({@code rubro.usaVariantes()}) en lugar de por el rubro, así agregar uno nuevo es
 * sumar un valor acá y sus plantillas.
 */
public enum Rubro {

    RESTAURANTE("Restaurante", true, false, false,
            List.of("Entradas", "Platos principales", "Postres", "Bebidas")),
    ROPA("Tienda de ropa", false, true, true,
            List.of("Remeras", "Pantalones", "Abrigos", "Accesorios")),
    ARTICULOS("Venta de artículos", false, false, true,
            List.of("Destacados", "Hogar", "Regalería"));

    private final String etiqueta;
    private final boolean usaMesas;
    private final boolean usaVariantes;
    private final boolean usaBuscador;
    private final List<String> categoriasSugeridas;

    Rubro(String etiqueta, boolean usaMesas, boolean usaVariantes, boolean usaBuscador,
            List<String> categoriasSugeridas) {
        this.etiqueta = etiqueta;
        this.usaMesas = usaMesas;
        this.usaVariantes = usaVariantes;
        this.usaBuscador = usaBuscador;
        this.categoriasSugeridas = categoriasSugeridas;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Menú con código QR por mesa. */
    public boolean usaMesas() {
        return usaMesas;
    }

    /** Productos con variantes (talle, color). */
    public boolean usaVariantes() {
        return usaVariantes;
    }

    /** Catálogo con buscador y filtros. */
    public boolean usaBuscador() {
        return usaBuscador;
    }

    /** Categorías que se crean al dar de alta un comercio de este rubro. */
    public List<String> getCategoriasSugeridas() {
        return categoriasSugeridas;
    }
}
