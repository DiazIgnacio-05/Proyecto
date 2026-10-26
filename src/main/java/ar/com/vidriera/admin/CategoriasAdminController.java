package ar.com.vidriera.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categorias")
class CategoriasAdminController {

    private final CategoriaAdminService categorias;

    CategoriasAdminController(CategoriaAdminService categorias) {
        this.categorias = categorias;
    }

    @ModelAttribute("seccion")
    String seccion() {
        return "categorias";
    }

    @GetMapping
    String lista(Model model) {
        model.addAttribute("categorias", categorias.listar());
        return "admin/categorias";
    }

    @PostMapping
    String crear(@RequestParam String nombre, RedirectAttributes flash) {
        categorias.crear(nombre);
        flash.addFlashAttribute("ok", "Categoría creada");
        return "redirect:/admin/categorias";
    }

    @PostMapping("/{id}")
    String actualizar(@PathVariable Long id, @RequestParam String nombre, @RequestParam int orden,
            @RequestParam(defaultValue = "false") boolean activa, RedirectAttributes flash) {
        categorias.actualizar(id, nombre, orden, activa);
        flash.addFlashAttribute("ok", "Categoría actualizada");
        return "redirect:/admin/categorias";
    }

    @PostMapping("/{id}/eliminar")
    String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        categorias.eliminar(id);
        flash.addFlashAttribute("ok", "Categoría eliminada");
        return "redirect:/admin/categorias";
    }
}
