package ar.com.vidriera.admin;

import java.io.IOException;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Productos del panel. Después de cada POST se redirige (patrón "Post/Redirect/Get"): así, si el
 * dueño recarga la página, el navegador no reenvía el formulario.
 */
@Controller
@RequestMapping("/admin/productos")
class ProductosAdminController {

    private final ProductoAdminService productos;
    private final CategoriaAdminService categorias;

    ProductosAdminController(ProductoAdminService productos, CategoriaAdminService categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    @ModelAttribute("seccion")
    String seccion() {
        return "productos";
    }

    @GetMapping
    String lista(Model model) {
        model.addAttribute("secciones", productos.listar());
        return "admin/productos/lista";
    }

    @GetMapping("/nuevo")
    String nuevo(@RequestParam(required = false) Long categoria, Model model) {
        ProductoForm form = new ProductoForm();
        form.setCategoriaId(categoria);
        return formulario(form, model);
    }

    @GetMapping("/{id}")
    String editar(@PathVariable Long id, Model model) {
        return formulario(productos.formulario(id), model);
    }

    @PostMapping
    String crear(@Valid @ModelAttribute("form") ProductoForm form, BindingResult errores, Model model,
            RedirectAttributes flash) {
        form.setId(null);
        return guardar(form, errores, model, flash);
    }

    @PostMapping("/{id}")
    String actualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ProductoForm form,
            BindingResult errores, Model model, RedirectAttributes flash) {
        form.setId(id); // el id de la URL manda, no un campo oculto del formulario
        return guardar(form, errores, model, flash);
    }

    @PostMapping("/{id}/disponible")
    String cambiarDisponible(@PathVariable Long id, RedirectAttributes flash) {
        boolean disponible = productos.cambiarDisponible(id);
        flash.addFlashAttribute("ok", disponible ? "Producto disponible otra vez" : "Producto marcado sin stock");
        return "redirect:/admin/productos#producto-" + id;
    }

    @PostMapping("/{id}/eliminar")
    String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        flash.addFlashAttribute("ok", "Se eliminó \"" + productos.eliminar(id) + "\"");
        return "redirect:/admin/productos";
    }

    @PostMapping("/{id}/fotos")
    String agregarFoto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto,
            RedirectAttributes flash) throws IOException {
        if (foto.isEmpty()) {
            flash.addFlashAttribute("error", "Elegí una foto para subir");
        } else {
            productos.agregarFoto(id, foto.getBytes());
            flash.addFlashAttribute("ok", "Foto agregada");
        }
        return "redirect:/admin/productos/" + id + "#fotos";
    }

    @PostMapping("/{id}/fotos/{fotoId}/eliminar")
    String quitarFoto(@PathVariable Long id, @PathVariable Long fotoId, RedirectAttributes flash) {
        productos.quitarFoto(id, fotoId);
        flash.addFlashAttribute("ok", "Foto eliminada");
        return "redirect:/admin/productos/" + id + "#fotos";
    }

    @PostMapping("/{id}/fotos/{fotoId}/principal")
    String fotoPrincipal(@PathVariable Long id, @PathVariable Long fotoId, RedirectAttributes flash) {
        productos.hacerPrincipal(id, fotoId);
        flash.addFlashAttribute("ok", "Foto principal actualizada");
        return "redirect:/admin/productos/" + id + "#fotos";
    }

    @PostMapping("/{id}/variantes")
    String agregarVariantes(@PathVariable Long id, @RequestParam(defaultValue = "") String talles,
            @RequestParam(defaultValue = "") String colores, RedirectAttributes flash) {
        int agregadas = productos.agregarVariantes(id, talles, colores);
        flash.addFlashAttribute("ok", agregadas == 0
                ? "Esas combinaciones ya estaban cargadas"
                : agregadas == 1 ? "Se agregó 1 combinación" : "Se agregaron " + agregadas + " combinaciones");
        return "redirect:/admin/productos/" + id + "#variantes";
    }

    @PostMapping("/{id}/variantes/{varianteId}/disponible")
    String cambiarVariante(@PathVariable Long id, @PathVariable Long varianteId, RedirectAttributes flash) {
        boolean disponible = productos.cambiarVariante(id, varianteId);
        flash.addFlashAttribute("ok", disponible ? "Disponible otra vez" : "Marcado sin stock");
        return "redirect:/admin/productos/" + id + "#variantes";
    }

    @PostMapping("/{id}/variantes/{varianteId}/eliminar")
    String quitarVariante(@PathVariable Long id, @PathVariable Long varianteId, RedirectAttributes flash) {
        productos.quitarVariante(id, varianteId);
        flash.addFlashAttribute("ok", "Combinación eliminada");
        return "redirect:/admin/productos/" + id + "#variantes";
    }

    private String guardar(ProductoForm form, BindingResult errores, Model model, RedirectAttributes flash) {
        if (!errores.hasErrors()) {
            try {
                Long id = productos.guardar(form);
                flash.addFlashAttribute("ok", "Producto guardado");
                return "redirect:/admin/productos/" + id;
            } catch (OperacionRechazadaException e) {
                errores.reject("rechazado", e.getMessage());
            }
        }
        return formulario(form, model);
    }

    private String formulario(ProductoForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("categorias", categorias.paraElegir());
        model.addAttribute("fotos", form.esNuevo() ? java.util.List.of() : productos.fotos(form.getId()));
        model.addAttribute("variantes", form.esNuevo() ? java.util.List.of() : productos.variantes(form.getId()));
        model.addAttribute("fotosMaximas", ProductoAdminService.FOTOS_MAXIMAS);
        return "admin/productos/formulario";
    }
}
