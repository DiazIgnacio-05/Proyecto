package ar.com.vidriera.superadmin;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ar.com.vidriera.catalogo.Categoria;
import ar.com.vidriera.catalogo.CategoriaRepository;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.Slug;
import ar.com.vidriera.pedido.NumeroWhatsApp;
import ar.com.vidriera.tenant.TenantContext;
import ar.com.vidriera.usuario.GeneradorContrasenas;
import ar.com.vidriera.usuario.Usuario;
import ar.com.vidriera.usuario.UsuarioRepository;

/**
 * Alta de un comercio nuevo: crea el comercio, el usuario del dueño con una contraseña temporal
 * (que deberá cambiar al entrar) y las categorías sugeridas para su rubro.
 *
 * <p>Se hace en dos transacciones porque Hibernate fija el tenant al abrir la sesión: primero se
 * crea el comercio (que todavía no tiene id), y después, ya con su id en {@link TenantContext}, sus
 * categorías. Si la segunda parte fallara, se borra el comercio recién creado.
 */
@Service
public class AltaComercioService {

    /** Lo que hay que pasarle al dueño. La contraseña no se guarda en ningún lado en texto plano. */
    public record ResultadoAlta(Long comercioId, String email, String contrasenaTemporal) {
    }

    private final ComercioRepository comercios;
    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final PasswordEncoder passwordEncoder;
    private final GeneradorContrasenas generador;
    private final TransactionTemplate transacciones;
    private final Clock reloj;

    public AltaComercioService(ComercioRepository comercios, UsuarioRepository usuarios,
            CategoriaRepository categorias, PasswordEncoder passwordEncoder, GeneradorContrasenas generador,
            TransactionTemplate transacciones, Clock reloj) {
        this.comercios = comercios;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.passwordEncoder = passwordEncoder;
        this.generador = generador;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public ResultadoAlta darDeAlta(AltaComercioForm form) {
        String slug = slugValido(form);
        String email = emailValido(form.getEmailDueno());
        validarWhatsApp(form.getWhatsapp());
        String temporal = generador.generar();

        Long comercioId = transacciones.execute(estado -> {
            Comercio comercio = new Comercio(slug, form.getNombre(), form.getRubro(), form.getWhatsapp().strip(),
                    LocalDate.now(reloj));
            comercio.setVencimientoAbono(form.getVencimientoAbono());
            comercios.save(comercio);
            Usuario dueno = Usuario.duenoDe(comercio, email, passwordEncoder.encode(temporal),
                    form.getNombreDueno().strip());
            dueno.exigirCambioDePassword();
            usuarios.save(dueno);
            return comercio.getId();
        });

        try {
            TenantContext.ejecutarComo(comercioId, () -> transacciones.execute(estado -> {
                List<String> sugeridas = form.getRubro().getCategoriasSugeridas();
                for (int orden = 0; orden < sugeridas.size(); orden++) {
                    categorias.save(new Categoria(sugeridas.get(orden), orden + 1));
                }
                return null;
            }));
        } catch (RuntimeException e) {
            transacciones.executeWithoutResult(estado -> comercios.deleteById(comercioId));
            throw e;
        }
        return new ResultadoAlta(comercioId, email, temporal);
    }

    private String slugValido(AltaComercioForm form) {
        String slug = form.getSlug() == null || form.getSlug().isBlank()
                ? Slug.sugerir(form.getNombre())
                : form.getSlug().strip().toLowerCase();
        if (!Slug.esValido(slug)) {
            throw new AltaRechazadaException("El subdominio \"" + slug
                    + "\" no es válido: usá minúsculas, números y guiones (sin espacios ni tildes)");
        }
        if (Slug.esReservado(slug)) {
            throw new AltaRechazadaException("El subdominio \"" + slug + "\" está reservado por la plataforma");
        }
        if (comercios.existsBySlug(slug)) {
            throw new AltaRechazadaException("Ya hay un comercio con el subdominio \"" + slug + "\"");
        }
        return slug;
    }

    private String emailValido(String email) {
        String normalizado = Usuario.normalizarEmail(email);
        if (usuarios.existsByEmail(normalizado)) {
            throw new AltaRechazadaException("Ya existe un usuario con el email " + normalizado);
        }
        return normalizado;
    }

    private static void validarWhatsApp(String whatsapp) {
        try {
            NumeroWhatsApp.de(whatsapp);
        } catch (IllegalArgumentException e) {
            throw new AltaRechazadaException("No reconocemos el WhatsApp \"" + whatsapp
                    + "\". Escribilo con código de área, ej. 383 4123456");
        }
    }
}
