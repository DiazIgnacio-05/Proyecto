package ar.com.vidriera.tenant;

/**
 * Resultado de analizar el host de un request: a quién va dirigido.
 *
 * <p>Es una interfaz {@code sealed}: solo pueden existir estas tres variantes, y el compilador
 * avisa si un {@code switch} se olvida de alguna.
 */
public sealed interface DestinoHost {

    /** El dominio base o un subdominio reservado (www, app...): landing y paneles. */
    record Plataforma() implements DestinoHost {
    }

    /** Un subdominio de la plataforma, por ejemplo {@code sabores-del-valle.vidriera.com.ar}. */
    record SitioDeComercio(String slug) implements DestinoHost {
    }

    /** Un dominio propio de un comercio (módulo pago), por ejemplo {@code www.saboresdelvalle.com}. */
    record DominioPropio(String host) implements DestinoHost {
    }
}
