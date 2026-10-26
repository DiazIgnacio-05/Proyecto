package ar.com.vidriera.usuario;

public enum Rol {

    /** Dueño de la plataforma: gestiona todos los comercios. */
    SUPER_ADMIN,
    /** Dueño (o encargado) de un comercio: gestiona solo su sitio. */
    DUENO;

    /** Nombre de la autoridad que usa Spring Security ({@code hasRole("DUENO")}). */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
