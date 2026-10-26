package ar.com.vidriera.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Pantalla de inicio del panel del comercio. */
@Controller
class PanelController {

    private final ComercioAdminService comercio;

    PanelController(ComercioAdminService comercio) {
        this.comercio = comercio;
    }

    @GetMapping("/admin")
    String inicio(Model model) {
        model.addAttribute("resumen", comercio.resumen());
        model.addAttribute("seccion", "inicio");
        return "admin/inicio";
    }
}
