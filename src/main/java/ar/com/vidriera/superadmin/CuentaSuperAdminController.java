package ar.com.vidriera.superadmin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.vidriera.usuario.CuentaService;
import ar.com.vidriera.usuario.UsuarioAutenticado;

/** Cambio de contraseña del super-admin (la inicial viene de SUPERADMIN_PASSWORD). */
@Controller
@RequestMapping("/superadmin/cuenta")
class CuentaSuperAdminController {

    private final CuentaService cuentas;

    CuentaSuperAdminController(CuentaService cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping
    String formulario(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("largoMinimo", CuentaService.LARGO_MINIMO_PASSWORD);
        model.addAttribute("seccion", "cuenta");
        return "superadmin/cuenta";
    }

    @PostMapping
    String cambiarPassword(@AuthenticationPrincipal UsuarioAutenticado usuario, @RequestParam String actual,
            @RequestParam String nueva, @RequestParam String repetida, RedirectAttributes flash) {
        try {
            cuentas.cambiarPassword(usuario.getId(), actual, nueva, repetida);
            flash.addFlashAttribute("ok", "Contraseña actualizada");
        } catch (CuentaService.CambioRechazadoException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/superadmin/cuenta";
    }
}
