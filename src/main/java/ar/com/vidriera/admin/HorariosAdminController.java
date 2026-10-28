package ar.com.vidriera.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/horarios")
class HorariosAdminController {

    private final HorarioAdminService horarios;

    HorariosAdminController(HorarioAdminService horarios) {
        this.horarios = horarios;
    }

    @ModelAttribute("seccion")
    String seccion() {
        return "horarios";
    }

    @GetMapping
    String formulario(Model model) {
        model.addAttribute("form", horarios.formulario());
        return "admin/horarios";
    }

    @PostMapping
    String guardar(@ModelAttribute("form") HorariosForm form, BindingResult errores, Model model,
            RedirectAttributes flash) {
        if (errores.hasErrors()) {
            errores.reject("hora", "Revisá las horas: usá el formato 20:30");
            return "admin/horarios";
        }
        try {
            horarios.guardar(form);
        } catch (OperacionRechazadaException e) {
            errores.reject("rechazado", e.getMessage());
            return "admin/horarios";
        }
        flash.addFlashAttribute("ok", "Horarios guardados");
        return "redirect:/admin/horarios";
    }
}
