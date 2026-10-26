package ar.com.vidriera.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.vidriera.usuario.CuentaService;
import ar.com.vidriera.usuario.UsuarioAutenticado;

/** "Mi cuenta": cambio de contraseña. Si la contraseña es temporal, el panel obliga a pasar por acá. */
@Controller
@RequestMapping("/admin/cuenta")
class CuentaController {

    private final CuentaService cuentas;
    private final SecurityContextRepository contextos;

    CuentaController(CuentaService cuentas, SecurityContextRepository contextos) {
        this.cuentas = cuentas;
        this.contextos = contextos;
    }

    @ModelAttribute("seccion")
    String seccion() {
        return "cuenta";
    }

    @GetMapping
    String formulario(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("largoMinimo", CuentaService.LARGO_MINIMO_PASSWORD);
        return "admin/cuenta";
    }

    @PostMapping("/password")
    String cambiarPassword(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam String actual, @RequestParam String nueva, @RequestParam String repetida,
            HttpServletRequest request, HttpServletResponse response, RedirectAttributes flash) {
        try {
            cuentas.cambiarPassword(usuario.getId(), actual, nueva, repetida);
        } catch (CuentaService.CambioRechazadoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/cuenta";
        }
        // Se actualiza el usuario guardado en la sesión: ya no tiene que cambiar la contraseña.
        UsuarioAutenticado actualizado = usuario.conPasswordCambiada();
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                actualizado, null, actualizado.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto, request, response);

        flash.addFlashAttribute("ok", "Contraseña actualizada");
        return "redirect:/admin";
    }
}
