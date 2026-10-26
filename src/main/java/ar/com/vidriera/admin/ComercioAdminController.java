package ar.com.vidriera.admin;

import java.io.IOException;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** "Mi comercio": datos de contacto, apariencia, redes, SEO y opciones de pedido. */
@Controller
@RequestMapping("/admin/comercio")
class ComercioAdminController {

    private final ComercioAdminService comercio;

    ComercioAdminController(ComercioAdminService comercio) {
        this.comercio = comercio;
    }

    @ModelAttribute("seccion")
    String seccion() {
        return "comercio";
    }

    @GetMapping
    String formulario(Model model) {
        model.addAttribute("form", comercio.formulario());
        return "admin/comercio";
    }

    @PostMapping
    String guardar(@Valid @ModelAttribute("form") ComercioForm form, BindingResult errores,
            RedirectAttributes flash) {
        if (!errores.hasErrors()) {
            try {
                comercio.guardar(form);
                flash.addFlashAttribute("ok", "Datos guardados. Ya se ven en tu sitio.");
                return "redirect:/admin/comercio";
            } catch (OperacionRechazadaException e) {
                errores.reject("rechazado", e.getMessage());
            }
        }
        return "admin/comercio";
    }

    @PostMapping("/logo")
    String logo(@RequestParam("archivo") MultipartFile archivo, RedirectAttributes flash) throws IOException {
        if (archivo.isEmpty()) {
            flash.addFlashAttribute("error", "Elegí una imagen para el logo");
        } else {
            comercio.cambiarLogo(archivo.getBytes());
            flash.addFlashAttribute("ok", "Logo actualizado");
        }
        return "redirect:/admin/comercio#imagenes";
    }

    @PostMapping("/logo/eliminar")
    String quitarLogo(RedirectAttributes flash) {
        comercio.quitarLogo();
        flash.addFlashAttribute("ok", "Logo eliminado");
        return "redirect:/admin/comercio#imagenes";
    }

    @PostMapping("/portada")
    String portada(@RequestParam("archivo") MultipartFile archivo, RedirectAttributes flash) throws IOException {
        if (archivo.isEmpty()) {
            flash.addFlashAttribute("error", "Elegí una imagen para la portada");
        } else {
            comercio.cambiarPortada(archivo.getBytes());
            flash.addFlashAttribute("ok", "Portada actualizada");
        }
        return "redirect:/admin/comercio#imagenes";
    }

    @PostMapping("/portada/eliminar")
    String quitarPortada(RedirectAttributes flash) {
        comercio.quitarPortada();
        flash.addFlashAttribute("ok", "Portada eliminada");
        return "redirect:/admin/comercio#imagenes";
    }
}
