package ar.com.vidriera.admin;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Un archivo de más de 10 MB se rechaza antes de llegar a cualquier controlador, por eso este
 * manejador es global (no restringido al paquete del panel como {@link PanelAdvice}).
 */
@ControllerAdvice
class ArchivoGrandeAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    String archivoGrande(HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "La imagen pesa más de 10 MB. Probá con una foto más liviana.");
        return "redirect:" + PanelAdvice.volverA(request);
    }
}
