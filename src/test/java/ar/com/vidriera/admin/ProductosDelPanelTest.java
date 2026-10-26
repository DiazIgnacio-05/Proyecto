package ar.com.vidriera.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import ar.com.vidriera.catalogo.ImagenProducto;
import ar.com.vidriera.catalogo.Producto;
import ar.com.vidriera.config.VidrieraProperties;
import ar.com.vidriera.tenant.TenantContext;

/** Alta, edición, stock, fotos y baja de productos desde el panel, de punta a punta. */
class ProductosDelPanelTest extends PruebaDelPanel {

    @Autowired
    private VidrieraProperties propiedades;

    @Test
    void creaUnProductoInterpretandoElPrecioConPuntos() throws Exception {
        ComercioDePrueba c = crearComercio("alta");
        String destino = mvc.perform(post("/admin/productos").header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf())
                        .param("categoriaId", c.categoriaId().toString())
                        .param("nombre", "Napolitana")
                        .param("precio", "12.500")
                        .param("descripcion", "Tomate en rodajas y ajo")
                        .param("disponible", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("ok", "Producto guardado"))
                .andReturn().getResponse().getRedirectedUrl();

        Long id = Long.valueOf(destino.substring(destino.lastIndexOf('/') + 1));
        Producto creado = producto(c, id);
        assertThat(creado.getNombre()).isEqualTo("Napolitana");
        assertThat(creado.getPrecio()).isEqualByComparingTo("12500");
        assertThat(creado.getComercioId()).isEqualTo(c.id());
    }

    @Test
    void muestraLosErroresDelFormularioSinGuardar() throws Exception {
        ComercioDePrueba c = crearComercio("errores");
        mvc.perform(post("/admin/productos").header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf())
                        .param("categoriaId", c.categoriaId().toString())
                        .param("nombre", "")
                        .param("precio", "doce mil"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribí el nombre del producto")));

        mvc.perform(post("/admin/productos").header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf())
                        .param("categoriaId", c.categoriaId().toString())
                        .param("nombre", "Fugazzeta")
                        .param("precio", "doce mil"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No entendimos el precio")));
        assertThat(TenantContext.ejecutarComo(c.id(), () -> productos.count())).isEqualTo(1);
    }

    @Test
    void marcaYDesmarcaSinStock() throws Exception {
        ComercioDePrueba c = crearComercio("stock");
        mvc.perform(post("/admin/productos/{id}/disponible", c.productoId())
                        .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()))
                .andExpect(flash().attribute("ok", "Producto marcado sin stock"));
        assertThat(producto(c, c.productoId()).isDisponible()).isFalse();

        mvc.perform(post("/admin/productos/{id}/disponible", c.productoId())
                .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()));
        assertThat(producto(c, c.productoId()).isDisponible()).isTrue();
    }

    @Test
    void subeUnaFotoLaOptimizaYSeVeEnElSitioPublico() throws Exception {
        ComercioDePrueba c = crearComercio("foto");
        MockMultipartFile foto = new MockMultipartFile("foto", "pizza.jpg", "image/jpeg", jpeg(2400, 1800));

        mvc.perform(multipart("/admin/productos/{id}/fotos", c.productoId()).file(foto)
                        .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()))
                .andExpect(flash().attribute("ok", "Foto agregada"));

        String clave = enTransaccion(c,
                () -> productos.findById(c.productoId()).orElseThrow().getImagenes().stream()
                        .map(ImagenProducto::getClave).findFirst().orElseThrow());
        Path chica = archivo(clave + "-480.jpg");
        Path grande = archivo(clave + "-1200.jpg");
        assertThat(clave).startsWith("c" + c.id() + "/productos/");
        assertThat(chica).exists();
        assertThat(grande).exists();
        assertThat(ImageIO.read(chica.toFile()).getWidth()).isEqualTo(480);
        assertThat(Files.size(grande)).isLessThan(foto.getSize());

        // El sitio público muestra la miniatura y el archivo se sirve con caché larga.
        mvc.perform(get("/").header("Host", c.slug() + ".localhost"))
                .andExpect(content().string(containsString("/imagenes/" + clave + "-480.jpg")));
        mvc.perform(get("/imagenes/" + clave + "-480.jpg").header("Host", c.slug() + ".localhost"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")));

        // Al eliminar la foto se borran los archivos.
        Long fotoId = enTransaccion(c,
                () -> productos.findById(c.productoId()).orElseThrow().getImagenes().getFirst().getId());
        mvc.perform(post("/admin/productos/{id}/fotos/{f}/eliminar", c.productoId(), fotoId)
                        .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()))
                .andExpect(flash().attribute("ok", "Foto eliminada"));
        assertThat(chica).doesNotExist();
        assertThat(grande).doesNotExist();
    }

    @Test
    void rechazaArchivosQueNoSonImagenes() throws Exception {
        ComercioDePrueba c = crearComercio("noimagen");
        MockMultipartFile falso = new MockMultipartFile("foto", "virus.jpg", "image/jpeg", "no soy una foto".getBytes());
        mvc.perform(multipart("/admin/productos/{id}/fotos", c.productoId()).file(falso)
                        .header("Host", HOST_PANEL).header("Referer", "http://app.localhost/admin/productos/" + c.productoId())
                        .with(comoDueno(c)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("no es una imagen")));
        assertThat(producto(c, c.productoId())).isNotNull();
        assertThat(enTransaccion(c,
                () -> productos.findById(c.productoId()).orElseThrow().getImagenes().size())).isZero();
    }

    @Test
    void noBorraCategoriasConProductos() throws Exception {
        ComercioDePrueba c = crearComercio("categoria");
        mvc.perform(post("/admin/categorias/{id}/eliminar", c.categoriaId())
                        .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("tiene productos")));
        assertThat(TenantContext.ejecutarComo(c.id(), () -> categorias.existsById(c.categoriaId()))).isTrue();
    }

    @Test
    void eliminaUnProducto() throws Exception {
        ComercioDePrueba c = crearComercio("baja");
        mvc.perform(post("/admin/productos/{id}/eliminar", c.productoId())
                        .header("Host", HOST_PANEL).with(comoDueno(c)).with(csrf()))
                .andExpect(flash().attribute("ok", "Se eliminó \"Muzzarella\""));
        assertThat(producto(c, c.productoId())).isNull();
    }

    @Test
    void todasLasPantallasDelPanelCargan() throws Exception {
        ComercioDePrueba c = crearComercio("pantallas");
        for (String ruta : new String[] {"/admin", "/admin/productos", "/admin/productos/nuevo",
                "/admin/productos/" + c.productoId(), "/admin/categorias", "/admin/horarios",
                "/admin/comercio", "/admin/mesas", "/admin/cuenta"}) {
            mvc.perform(get(ruta).header("Host", HOST_PANEL).with(comoDueno(c)))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/admin/mesas/2/qr.svg").header("Host", HOST_PANEL).with(comoDueno(c)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<svg")));
        mvc.perform(get("/admin/mesas/5/qr.svg").header("Host", HOST_PANEL).with(comoDueno(c)))
                .andExpect(status().isNotFound());
    }

    private Path archivo(String ruta) {
        return Path.of(propiedades.imagenes().directorio()).resolve(ruta);
    }

    private static byte[] jpeg(int ancho, int alto) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        // Ruido de colores para que el JPEG original sea pesado, como una foto real.
        java.util.Random azar = new java.util.Random(7);
        for (int y = 0; y < alto; y += 8) {
            for (int x = 0; x < ancho; x += 8) {
                g.setColor(new Color(azar.nextInt(0xFFFFFF)));
                g.fillRect(x, y, 8, 8);
            }
        }
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "jpg", salida);
        return salida.toByteArray();
    }
}
