package ar.com.vidriera.usuario;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Página de login propia (en español). El POST de /login lo procesa Spring Security. */
@Controller
class LoginController {

    @GetMapping("/login")
    String login(Authentication autenticacion) {
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario.getRol() == Rol.SUPER_ADMIN ? "redirect:/superadmin" : "redirect:/admin";
        }
        return "plataforma/login";
    }
}
