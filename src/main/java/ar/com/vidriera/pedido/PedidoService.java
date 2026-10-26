package ar.com.vidriera.pedido;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.vidriera.catalogo.Producto;
import ar.com.vidriera.catalogo.ProductoRepository;
import ar.com.vidriera.comercio.Comercio;

/**
 * Convierte lo que eligió el cliente en un link de WhatsApp. Los nombres y precios salen de la base
 * (filtrada por el comercio actual), nunca del navegador: así nadie puede mandar un precio inventado
 * ni pedir un producto de otro comercio.
 */
@Service
public class PedidoService {

    private final ProductoRepository productos;

    public PedidoService(ProductoRepository productos) {
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public String linkDeWhatsApp(Comercio comercio, PedidoForm form) {
        Pedido pedido = new Pedido(comercio.getNombre(), armarLineas(form.getLineas()), entrega(comercio, form));
        return NumeroWhatsApp.de(comercio.getContacto().getWhatsapp())
                .linkConMensaje(MensajeWhatsApp.de(pedido));
    }

    private List<LineaPedido> armarLineas(List<PedidoForm.Linea> lineasForm) {
        // Si el mismo producto viene dos veces, se suman las cantidades (respetando el orden).
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (PedidoForm.Linea linea : lineasForm) {
            cantidades.merge(linea.getProductoId(), linea.getCantidad(), Integer::sum);
        }
        Map<Long, Producto> encontrados = productos.findAllById(cantidades.keySet()).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        List<LineaPedido> lineas = new ArrayList<>();
        cantidades.forEach((id, cantidad) -> {
            Producto producto = encontrados.get(id);
            if (producto == null || !producto.sePuedePedir()) {
                String nombre = producto == null ? "Un producto" : producto.getNombre();
                throw new PedidoInvalidoException(nombre + " ya no está disponible. Revisá tu pedido.");
            }
            lineas.add(new LineaPedido(producto.getNombre(), cantidad, producto.getPrecio()));
        });
        return lineas;
    }

    private DatosEntrega entrega(Comercio comercio, PedidoForm form) {
        return switch (form.getModalidad()) {
            case MESA -> {
                if (form.getMesa() == null || !comercio.tieneMesa(form.getMesa())) {
                    throw new PedidoInvalidoException("La mesa indicada no existe");
                }
                yield DatosEntrega.enMesa(form.getMesa(), form.getNombre(), form.getComentario());
            }
            case RETIRO -> DatosEntrega.retiro(form.getNombre(), form.getComentario());
            case ENVIO -> {
                if (!comercio.isOfreceEnvio()) {
                    throw new PedidoInvalidoException("Este comercio no hace envíos");
                }
                yield DatosEntrega.envio(form.getNombre(), form.getDireccion(), form.getComentario());
            }
        };
    }
}
