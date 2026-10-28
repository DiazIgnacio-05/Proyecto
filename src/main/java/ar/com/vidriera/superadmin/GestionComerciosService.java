package ar.com.vidriera.superadmin;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.comercio.Comercio;
import ar.com.vidriera.comercio.ComercioRepository;
import ar.com.vidriera.comercio.EstadoAbono;
import ar.com.vidriera.comercio.EstadoComercio;
import ar.com.vidriera.comercio.Modulo;
import ar.com.vidriera.tenant.TenantContext;
import ar.com.vidriera.usuario.GeneradorContrasenas;
import ar.com.vidriera.usuario.LimitadorIntentosLogin;
import ar.com.vidriera.usuario.Rol;
import ar.com.vidriera.usuario.Usuario;
import ar.com.vidriera.usuario.UsuarioRepository;

/**
 * Todo lo que hace el super-admin sobre los comercios: verlos, cambiar su estado, registrar pagos,
 * activar módulos y resetear la contraseña del dueño. {@code Comercio} y {@code Usuario} no tienen
 * filtro de tenant, así que acá se ven todos.
 */
@Service
public class GestionComerciosService {

    /** Un renglón del listado. */
    public record ComercioFila(Long id, String nombre, String slug, String rubro, EstadoComercio estado,
            EstadoAbono abono, long productos, String emailDueno) {
    }

    /** Totales para las tarjetas de arriba del listado. */
    public record Resumen(long activos, long suspendidos, long bajas, long vencidosActivos) {
    }

    public record UsuarioFila(Long id, String email, String nombre, boolean debeCambiarPassword) {
    }

    public record Detalle(Comercio comercio, EstadoAbono abono, long productos, List<UsuarioFila> usuarios) {
    }

    /** Filtros del listado. */
    public enum Filtro {
        TODOS("Todos"), ACTIVOS("Activos"), VENCIDOS("Abono vencido"), SUSPENDIDOS("Suspendidos"), BAJAS("Bajas");

        private final String etiqueta;

        Filtro(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    private final ComercioRepository comercios;
    private final UsuarioRepository usuarios;
    private final ProductoRepository productos;
    private final PasswordEncoder passwordEncoder;
    private final GeneradorContrasenas generador;
    private final LimitadorIntentosLogin limitador;
    private final Clock reloj;
    private final TransactionTemplate transaccionNueva;

    public GestionComerciosService(ComercioRepository comercios, UsuarioRepository usuarios,
            ProductoRepository productos, PasswordEncoder passwordEncoder, GeneradorContrasenas generador,
            LimitadorIntentosLogin limitador, Clock reloj, PlatformTransactionManager transacciones) {
        this.comercios = comercios;
        this.usuarios = usuarios;
        this.productos = productos;
        this.passwordEncoder = passwordEncoder;
        this.generador = generador;
        this.limitador = limitador;
        this.reloj = reloj;
        this.transaccionNueva = new TransactionTemplate(transacciones);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaccionNueva.setReadOnly(true);
    }

    @Transactional(readOnly = true)
    public List<ComercioFila> listar(Filtro filtro) {
        LocalDate hoy = hoy();
        Map<Long, Long> cantidades = productosPorComercio();
        Map<Long, String> duenos = usuarios.findAll().stream()
                .filter(u -> u.getRol() == Rol.DUENO)
                .collect(Collectors.toMap(u -> u.getComercio().getId(), Usuario::getEmail, (a, b) -> a));
        return comercios.findAllByOrderByNombreAsc().stream()
                .map(c -> new ComercioFila(c.getId(), c.getNombre(), c.getSlug(), c.getRubro().getEtiqueta(),
                        c.getEstado(), EstadoAbono.de(c.getVencimientoAbono(), hoy),
                        cantidades.getOrDefault(c.getId(), 0L), duenos.get(c.getId())))
                .filter(f -> cumple(f, filtro))
                // Los que necesitan atención primero: vencidos activos, después el resto.
                .sorted(Comparator.comparing((ComercioFila f) -> !(f.estado() == EstadoComercio.ACTIVO
                        && f.abono().vencido())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        LocalDate hoy = hoy();
        List<Comercio> todos = comercios.findAll();
        return new Resumen(
                todos.stream().filter(c -> c.getEstado() == EstadoComercio.ACTIVO).count(),
                todos.stream().filter(c -> c.getEstado() == EstadoComercio.SUSPENDIDO).count(),
                todos.stream().filter(c -> c.getEstado() == EstadoComercio.BAJA).count(),
                todos.stream().filter(c -> c.getEstado() == EstadoComercio.ACTIVO
                        && EstadoAbono.de(c.getVencimientoAbono(), hoy).vencido()).count());
    }

    @Transactional(readOnly = true)
    public Detalle detalle(Long id) {
        Comercio comercio = buscar(id);
        List<UsuarioFila> filas = usuarios.findByComercioIdOrderByIdAsc(id).stream()
                .map(u -> new UsuarioFila(u.getId(), u.getEmail(), u.getNombre(), u.isDebeCambiarPassword()))
                .toList();
        return new Detalle(comercio, EstadoAbono.de(comercio.getVencimientoAbono(), hoy()),
                productosPorComercio().getOrDefault(id, 0L), filas);
    }

    @Transactional
    public void suspender(Long id) {
        Comercio comercio = buscar(id);
        if (comercio.getEstado() == EstadoComercio.BAJA) {
            throw new AltaRechazadaException("El comercio está dado de baja: reactivalo primero si querés suspenderlo");
        }
        comercio.suspender();
    }

    @Transactional
    public void reactivar(Long id) {
        buscar(id).reactivar();
    }

    @Transactional
    public void darDeBaja(Long id) {
        buscar(id).darDeBaja();
    }

    /** Suma un mes al abono (desde el vencimiento o desde hoy si ya venció) y reactiva si estaba suspendido. */
    @Transactional
    public LocalDate registrarPago(Long id) {
        Comercio comercio = buscar(id);
        if (comercio.getEstado() == EstadoComercio.BAJA) {
            throw new AltaRechazadaException("El comercio está dado de baja: reactivalo antes de registrar un pago");
        }
        LocalDate nuevo = EstadoAbono.despuesDePagarUnMes(comercio.getVencimientoAbono(), hoy());
        comercio.setVencimientoAbono(nuevo);
        if (comercio.getEstado() == EstadoComercio.SUSPENDIDO) {
            comercio.reactivar();
        }
        return nuevo;
    }

    @Transactional
    public void guardarModulos(Long id, Set<Modulo> elegidos, String dominioPropio) {
        Comercio comercio = buscar(id);
        Set<Modulo> activos = EnumSet.noneOf(Modulo.class);
        if (elegidos != null) {
            activos.addAll(elegidos);
        }
        for (Modulo modulo : Modulo.values()) {
            if (activos.contains(modulo)) {
                comercio.activarModulo(modulo);
            } else {
                comercio.desactivarModulo(modulo);
            }
        }
        String dominio = dominioPropio == null || dominioPropio.isBlank() ? null : dominioPropio.strip().toLowerCase();
        if (dominio != null) {
            if (!dominio.matches("^([a-z0-9]([a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}$")) {
                throw new AltaRechazadaException("El dominio \"" + dominio + "\" no es válido (ej.: www.tucomercio.com.ar)");
            }
            if (comercios.existsByDominioPropioAndIdNot(dominio, id)) {
                throw new AltaRechazadaException("El dominio " + dominio + " ya está asignado a otro comercio");
            }
        }
        comercio.setDominioPropio(dominio);
    }

    @Transactional
    public void guardarNotas(Long id, String notas) {
        String limpio = notas == null || notas.isBlank() ? null : notas.strip();
        if (limpio != null && limpio.length() > 2000) {
            throw new AltaRechazadaException("Las notas son demasiado largas (máx. 2000 caracteres)");
        }
        buscar(id).setNotasInternas(limpio);
    }

    /** Genera una contraseña temporal nueva para un usuario del comercio y la devuelve (se muestra una vez). */
    @Transactional
    public String resetearContrasena(Long comercioId, Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .filter(u -> u.getComercio() != null && u.getComercio().getId().equals(comercioId))
                .orElseThrow(() -> new AltaRechazadaException("Ese usuario no pertenece al comercio"));
        String temporal = generador.generar();
        usuario.cambiarPassword(passwordEncoder.encode(temporal));
        usuario.exigirCambioDePassword();
        limitador.registrarExito(usuario.getEmail()); // por si estaba bloqueado por intentos fallidos
        return temporal;
    }

    private static boolean cumple(ComercioFila fila, Filtro filtro) {
        return switch (filtro == null ? Filtro.TODOS : filtro) {
            case TODOS -> true;
            case ACTIVOS -> fila.estado() == EstadoComercio.ACTIVO;
            case VENCIDOS -> fila.estado() == EstadoComercio.ACTIVO && fila.abono().vencido();
            case SUSPENDIDOS -> fila.estado() == EstadoComercio.SUSPENDIDO;
            case BAJAS -> fila.estado() == EstadoComercio.BAJA;
        };
    }

    /**
     * Cantidad de productos de cada comercio. Es una de las pocas consultas que necesita ver los datos
     * de todos los comercios, por eso usa el modo sistema (sin filtro de tenant).
     */
    private Map<Long, Long> productosPorComercio() {
        // Transacción NUEVA: la del método que llama ya abrió su sesión "sin comercio", y Hibernate
        // fija el tenant al abrir la sesión. Esta se abre con el modo sistema ya establecido.
        return TenantContext.ejecutarComoSistema(() -> transaccionNueva.execute(estado ->
                productos.contarPorComercio().stream()
                        .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1]))));
    }

    private Comercio buscar(Long id) {
        return comercios.findById(id).orElseThrow(() -> new ComercioNoEncontradoException(id));
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
