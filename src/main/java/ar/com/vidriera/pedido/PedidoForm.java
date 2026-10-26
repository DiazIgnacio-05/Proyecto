package ar.com.vidriera.pedido;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que manda el navegador al confirmar el pedido. Spring completa este objeto con los campos del
 * formulario ({@code lineas[0].productoId}, {@code modalidad}, ...) y Bean Validation revisa las
 * anotaciones. Ojo: solo trae ids y cantidades; los precios y nombres se buscan en la base.
 */
public class PedidoForm {

    @NotEmpty(message = "El pedido está vacío")
    @Size(max = Pedido.LINEAS_MAXIMAS, message = "El pedido tiene demasiados productos")
    @Valid
    private List<Linea> lineas = new ArrayList<>();

    @NotNull(message = "Elegí cómo querés recibir el pedido")
    private ModalidadPedido modalidad;

    private Integer mesa;

    @Size(max = 80, message = "El nombre es demasiado largo")
    private String nombre;

    @Size(max = 200, message = "La dirección es demasiado larga")
    private String direccion;

    @Size(max = 300, message = "El comentario es demasiado largo")
    private String comentario;

    public static class Linea {

        @NotNull
        private Long productoId;

        @Min(value = 1, message = "La cantidad mínima es 1")
        @Max(value = LineaPedido.CANTIDAD_MAXIMA, message = "La cantidad máxima es 99")
        private int cantidad;

        public Linea() {
        }

        public Linea(Long productoId, int cantidad) {
            this.productoId = productoId;
            this.cantidad = cantidad;
        }

        public Long getProductoId() {
            return productoId;
        }

        public void setProductoId(Long productoId) {
            this.productoId = productoId;
        }

        public int getCantidad() {
            return cantidad;
        }

        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }
    }

    public List<Linea> getLineas() {
        return lineas;
    }

    public void setLineas(List<Linea> lineas) {
        this.lineas = lineas;
    }

    public ModalidadPedido getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadPedido modalidad) {
        this.modalidad = modalidad;
    }

    public Integer getMesa() {
        return mesa;
    }

    public void setMesa(Integer mesa) {
        this.mesa = mesa;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
