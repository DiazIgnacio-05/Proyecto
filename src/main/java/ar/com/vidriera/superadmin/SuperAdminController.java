package ar.com.vidriera.superadmin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Panel del super-admin. Por ahora solo la portada; la gestión de comercios llega en la Etapa 5. */
@Controller
class SuperAdminController {

    @GetMapping("/superadmin")
    String inicio() {
        return "superadmin/inicio";
    }
}
