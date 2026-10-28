package ar.com.vidriera.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.vidriera.catalogo.CategoriaRepository;
import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.Contacto;
import ar.com.vidriera.imagen.ServicioImagenes;
import ar.com.vidriera.pedido.NumeroWhatsApp;
import ar.com.vidriera.tenant.TenantContext;

/**
 * Datos del comercio logueado. {@code Comercio} no tiene filtro de tenant (es el tenant), así que
 * acá se busca SIEMPRE por el id de {@link TenantContext}, que puso el filtro del panel a partir del
 * usuario. Nunca por un id que venga del navegador.
 */
@Service
public class ComercioAdminService {

    public record Resumen(long productos, long sinStock, long categorias) {
    }

    private final ComercioRepository comercios;
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final ServicioImagenes imagenes;

    public ComercioAdminService(ComercioRepository comercios, ProductoRepository productos,
            CategoriaRepository categorias, ServicioImagenes imagenes) {
        this.comercios = comercios;
        this.productos = productos;
        this.categorias = categorias;
        this.imagenes = imagenes;
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        return new Resumen(productos.count(), productos.countByDisponibleFalse(), categorias.count());
    }

    @Transactional(readOnly = true)
    public ComercioForm formulario() {
        Comercio c = actual();
        ComercioForm form = new ComercioForm();
        form.setNombre(c.getNombre());
        form.setDescripcion(c.getDescripcion());
        form.setWhatsapp(c.getContacto().getWhatsapp());
        form.setTelefono(c.getContacto().getTelefono());
        form.setEmail(c.getContacto().getEmail());
        form.setDireccion(c.getContacto().getDireccion());
        form.setLatitud(c.getContacto().getLatitud());
        form.setLongitud(c.getContacto().getLongitud());
        form.setInstagram(c.getRedes().getInstagram());
        form.setFacebook(c.getRedes().getFacebook());
        form.setTiktok(c.getRedes().getTiktok());
        form.setColorPrimario(c.getApariencia().getColorPrimario());
        form.setColorSecundario(c.getApariencia().getColorSecundario());
        form.setSeoTitulo(c.getSeo().getTitulo());
        form.setSeoDescripcion(c.getSeo().getDescripcion());
        form.setOfreceEnvio(c.isOfreceEnvio());
        form.setCantidadMesas(c.getCantidadMesas());
        return form;
    }

    @Transactional
    public void guardar(ComercioForm form) {
        try {
            NumeroWhatsApp.de(form.getWhatsapp());
        } catch (IllegalArgumentException e) {
            throw new OperacionRechazadaException(
                    "No reconocemos el WhatsApp \"" + form.getWhatsapp() + "\". Escribilo con código de área, ej. 383 4123456");
        }
        if ((form.getLatitud() == null) != (form.getLongitud() == null)) {
            throw new OperacionRechazadaException("Para el mapa hacen falta la latitud y la longitud");
        }
        Comercio c = actual();
        c.setNombre(form.getNombre());
        c.setDescripcion(opcional(form.getDescripcion()));
        Contacto contacto = c.getContacto();
        contacto.setWhatsapp(form.getWhatsapp().strip());
        contacto.setTelefono(opcional(form.getTelefono()));
        contacto.setEmail(opcional(form.getEmail()));
        contacto.setDireccion(opcional(form.getDireccion()));
        contacto.setLatitud(form.getLatitud());
        contacto.setLongitud(form.getLongitud());
        c.getRedes().setInstagram(opcional(form.getInstagram()));
        c.getRedes().setFacebook(opcional(form.getFacebook()));
        c.getRedes().setTiktok(opcional(form.getTiktok()));
        c.getApariencia().setColorPrimario(form.getColorPrimario());
        c.getApariencia().setColorSecundario(form.getColorSecundario());
        c.getSeo().setTitulo(opcional(form.getSeoTitulo()));
        c.getSeo().setDescripcion(opcional(form.getSeoDescripcion()));
        c.setOfreceEnvio(form.isOfreceEnvio());
        c.setCantidadMesas(c.getRubro().usaMesas() ? form.getCantidadMesas() : 0);
    }

    @Transactional
    public void cambiarLogo(byte[] archivo) {
        Comercio c = actual();
        String anterior = c.getApariencia().getLogoUrl();
        c.getApariencia().setLogoUrl(imagenes.guardarLogo(c.getId(), archivo));
        AlConfirmar.ejecutar(() -> imagenes.eliminarPorUrl(anterior));
    }

    @Transactional
    public void cambiarPortada(byte[] archivo) {
        Comercio c = actual();
        String anterior = c.getApariencia().getPortadaUrl();
        c.getApariencia().setPortadaUrl(imagenes.guardarPortada(c.getId(), archivo));
        AlConfirmar.ejecutar(() -> imagenes.eliminarPorUrl(anterior));
    }

    @Transactional
    public void quitarLogo() {
        Comercio c = actual();
        String logo = c.getApariencia().getLogoUrl();
        AlConfirmar.ejecutar(() -> imagenes.eliminarPorUrl(logo));
        c.getApariencia().setLogoUrl(null);
    }

    @Transactional
    public void quitarPortada() {
        Comercio c = actual();
        String portada = c.getApariencia().getPortadaUrl();
        AlConfirmar.ejecutar(() -> imagenes.eliminarPorUrl(portada));
        c.getApariencia().setPortadaUrl(null);
    }

    private Comercio actual() {
        Long id = TenantContext.actual()
                .orElseThrow(() -> new IllegalStateException("No hay comercio en el contexto del panel"));
        return comercios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Comercio"));
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
