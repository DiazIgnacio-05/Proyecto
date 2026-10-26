package ar.com.vidriera.pedido;

import java.util.Objects;

/**
 * Cómo recibe el pedido el cliente. Las reglas: en mesa hace falta el número de mesa; para retiro y
 * envío, el nombre; para envío, además, la dirección.
 */
public record DatosEntrega(ModalidadPedido modalidad, Integer mesa, String nombre, String direccion,
        String comentario) {

    public DatosEntrega {
        Objects.requireNonNull(modalidad, "modalidad");
        nombre = limpiar(nombre);
        direccion = limpiar(direccion);
        comentario = limpiar(comentario);
        switch (modalidad) {
            case MESA -> {
                if (mesa == null || mesa < 1) {
                    throw new PedidoInvalidoException("Falta el número de mesa");
                }
                direccion = null;
            }
            case RETIRO -> {
                exigir(nombre, "Contanos tu nombre para el retiro");
                mesa = null;
                direccion = null;
            }
            case ENVIO -> {
                exigir(nombre, "Contanos tu nombre para el envío");
                exigir(direccion, "Falta la dirección de entrega");
                mesa = null;
            }
        }
    }

    public static DatosEntrega enMesa(int mesa, String nombre, String comentario) {
        return new DatosEntrega(ModalidadPedido.MESA, mesa, nombre, null, comentario);
    }

    public static DatosEntrega retiro(String nombre, String comentario) {
        return new DatosEntrega(ModalidadPedido.RETIRO, null, nombre, null, comentario);
    }

    public static DatosEntrega envio(String nombre, String direccion, String comentario) {
        return new DatosEntrega(ModalidadPedido.ENVIO, null, nombre, direccion, comentario);
    }

    private static void exigir(String valor, String mensaje) {
        if (valor == null) {
            throw new PedidoInvalidoException(mensaje);
        }
    }

    /** Recorta espacios y convierte el texto vacío en null. */
    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }
}
