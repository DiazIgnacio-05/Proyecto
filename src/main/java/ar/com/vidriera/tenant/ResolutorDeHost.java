package ar.com.vidriera.tenant;

import java.util.Locale;
import java.util.regex.Pattern;

import ar.com.vidriera.comercio.Slug;

/**
 * Decide a qué comercio va dirigido un request mirando solo el host. No accede a la base de datos,
 * así que se puede testear con tests unitarios simples.
 */
public class ResolutorDeHost {

    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private final String dominioBase;

    public ResolutorDeHost(String dominioBase) {
        this.dominioBase = normalizar(dominioBase);
        if (this.dominioBase.isEmpty()) {
            throw new IllegalArgumentException("El dominio base no puede estar vacío");
        }
    }

    public DestinoHost resolver(String host) {
        String h = normalizar(host);
        if (h.isEmpty() || h.equals(dominioBase) || esDireccionIp(h)) {
            return new DestinoHost.Plataforma();
        }
        String sufijo = "." + dominioBase;
        if (h.endsWith(sufijo)) {
            String subdominio = h.substring(0, h.length() - sufijo.length());
            if (subdominio.contains(".") || Slug.esReservado(subdominio) || !Slug.esValido(subdominio)) {
                return new DestinoHost.Plataforma();
            }
            return new DestinoHost.SitioDeComercio(subdominio);
        }
        return new DestinoHost.DominioPropio(h);
    }

    private static String normalizar(String host) {
        if (host == null) {
            return "";
        }
        String h = host.trim().toLowerCase(Locale.ROOT);
        return h.endsWith(".") ? h.substring(0, h.length() - 1) : h;
    }

    private static boolean esDireccionIp(String host) {
        return IPV4.matcher(host).matches() || host.contains(":") || host.startsWith("[");
    }
}
