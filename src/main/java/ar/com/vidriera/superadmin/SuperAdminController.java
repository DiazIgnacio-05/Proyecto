package ar.com.vidriera.superadmin;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.com.vidriera.comercio.Modulo;
import ar.com.vidriera.comercio.Rubro;
import ar.com.vidriera.comercio.UrlsDelComercio;
import ar.com.vidriera.pedido.NumeroWhatsApp;

/** Panel del super-admin: listado de comercios, alta, estados, abono, módulos y claves. */
@Controller
@RequestMapping("/superadmin")
class SuperAdminController {

    /**
     * Credenciales para pasarle al dueño. Viajan como mensaje flash: se muestran en la página
     * siguiente y después desaparecen (no quedan guardadas en ningún lado).
     */
    record Credenciales(String email, String contrasena, String urlPanel, String linkWhatsApp) {
    }

    private final GestionComerciosService gestion;
    private final AltaComercioService alta;
    private final UrlsDelComercio urls;
    private final Clock reloj;

    SuperAdminController(GestionComerciosService gestion, AltaComercioService alta, UrlsDelComercio urls,
            Clock reloj) {
        this.gestion = gestion;
        this.alta = alta;
        this.urls = urls;
        this.reloj = reloj;
    }

    @GetMapping
    String inicio(@RequestParam(defaultValue = "TODOS") GestionComerciosService.Filtro filtro, Model model) {
        model.addAttribute("filtro", filtro);
        model.addAttribute("filtros", GestionComerciosService.Filtro.values());
        model.addAttribute("resumen", gestion.resumen());
        model.addAttribute("comercios", gestion.listar(filtro));
        model.addAttribute("seccion", "comercios");
        return "superadmin/inicio";
    }

    @GetMapping("/comercios/nuevo")
    String nuevo(Model model) {
        AltaComercioForm form = new AltaComercioForm();
        form.setVencimientoAbono(LocalDate.now(reloj).plusMonths(1));
        return formularioAlta(form, model);
    }

    @PostMapping("/comercios")
    String crear(@Valid @ModelAttribute("form") AltaComercioForm form, BindingResult errores, Model model,
            HttpServletRequest request, RedirectAttributes flash) {
        if (!errores.hasErrors()) {
            try {
                AltaComercioService.ResultadoAlta resultado = alta.darDeAlta(form);
                flash.addFlashAttribute("ok", "Comercio creado. Pasale estos datos al dueño.");
                flash.addFlashAttribute("credenciales", credenciales(resultado.email(), resultado.contrasenaTemporal(),
                        form.getWhatsapp(), request));
                return "redirect:/superadmin/comercios/" + resultado.comercioId();
            } catch (AltaRechazadaException e) {
                errores.reject("rechazado", e.getMessage());
            }
        }
        return formularioAlta(form, model);
    }

    @GetMapping("/comercios/{id}")
    String detalle(@PathVariable Long id, Model model, HttpServletRequest request) {
        GestionComerciosService.Detalle detalle = gestion.detalle(id);
        model.addAttribute("detalle", detalle);
        model.addAttribute("c", detalle.comercio());
        model.addAttribute("urlSitio", urls.sitio(detalle.comercio(), request));
        model.addAttribute("modulos", Modulo.values());
        model.addAttribute("seccion", "comercios");
        return "superadmin/detalle";
    }

    @PostMapping("/comercios/{id}/suspender")
    String suspender(@PathVariable Long id, RedirectAttributes flash) {
        gestion.suspender(id);
        flash.addFlashAttribute("ok", "Sitio suspendido: muestra \"no disponible\" y el panel queda en solo lectura");
        return volver(id);
    }

    @PostMapping("/comercios/{id}/reactivar")
    String reactivar(@PathVariable Long id, RedirectAttributes flash) {
        gestion.reactivar(id);
        flash.addFlashAttribute("ok", "Comercio activo otra vez");
        return volver(id);
    }

    @PostMapping("/comercios/{id}/baja")
    String darDeBaja(@PathVariable Long id, RedirectAttributes flash) {
        gestion.darDeBaja(id);
        flash.addFlashAttribute("ok", "Comercio dado de baja. Sus datos se conservan por si vuelve.");
        return volver(id);
    }

    @PostMapping("/comercios/{id}/pago")
    String registrarPago(@PathVariable Long id, RedirectAttributes flash) {
        LocalDate vence = gestion.registrarPago(id);
        flash.addFlashAttribute("ok", "Pago registrado. El abono vence el "
                + String.format("%02d/%02d/%d", vence.getDayOfMonth(), vence.getMonthValue(), vence.getYear()) + ".");
        return volver(id);
    }

    @PostMapping("/comercios/{id}/modulos")
    String guardarModulos(@PathVariable Long id, @RequestParam(name = "modulo", required = false) Set<Modulo> modulos,
            @RequestParam(defaultValue = "") String dominioPropio, RedirectAttributes flash) {
        gestion.guardarModulos(id, modulos, dominioPropio);
        flash.addFlashAttribute("ok", "Módulos guardados");
        return volver(id);
    }

    @PostMapping("/comercios/{id}/notas")
    String guardarNotas(@PathVariable Long id, @RequestParam(defaultValue = "") String notas, RedirectAttributes flash) {
        gestion.guardarNotas(id, notas);
        flash.addFlashAttribute("ok", "Notas guardadas");
        return volver(id);
    }

    @PostMapping("/comercios/{id}/usuarios/{usuarioId}/contrasena")
    String resetearContrasena(@PathVariable Long id, @PathVariable Long usuarioId, HttpServletRequest request,
            RedirectAttributes flash) {
        String temporal = gestion.resetearContrasena(id, usuarioId);
        GestionComerciosService.Detalle detalle = gestion.detalle(id);
        String email = detalle.usuarios().stream().filter(u -> u.id().equals(usuarioId)).findFirst()
                .map(GestionComerciosService.UsuarioFila::email).orElse("");
        flash.addFlashAttribute("ok", "Se generó una contraseña temporal nueva.");
        flash.addFlashAttribute("credenciales", credenciales(email, temporal,
                detalle.comercio().getContacto().getWhatsapp(), request));
        return volver(id);
    }

    @ExceptionHandler(AltaRechazadaException.class)
    String rechazada(AltaRechazadaException e, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", e.getMessage());
        String anterior = request.getHeader("Referer");
        int ruta = anterior == null ? -1 : anterior.indexOf("/superadmin");
        return "redirect:" + (ruta >= 0 ? anterior.substring(ruta) : "/superadmin");
    }

    private String formularioAlta(AltaComercioForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("rubros", Rubro.values());
        model.addAttribute("seccion", "nuevo");
        return "superadmin/nuevo";
    }

    private Credenciales credenciales(String email, String contrasena, String whatsappComercio,
            HttpServletRequest request) {
        String urlPanel = ServletUriComponentsBuilder.fromContextPath(request).path("/login").build().toUriString();
        String mensaje = "¡Hola! Ya está listo tu panel para administrar tu sitio.\n"
                + "Entrá a " + urlPanel + "\n"
                + "Email: " + email + "\n"
                + "Contraseña temporal: " + contrasena + "\n"
                + "Al entrar te va a pedir que elijas una contraseña nueva.";
        String link;
        try {
            link = NumeroWhatsApp.de(whatsappComercio).linkConMensaje(mensaje);
        } catch (IllegalArgumentException e) {
            link = null;
        }
        return new Credenciales(email, contrasena, urlPanel, link);
    }

    private static String volver(Long id) {
        return "redirect:/superadmin/comercios/" + id;
    }

}
