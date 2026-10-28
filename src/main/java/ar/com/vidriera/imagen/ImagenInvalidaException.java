package ar.com.vidriera.imagen;

/** El archivo subido no es una imagen que podamos usar; el mensaje se le muestra al usuario. */
public class ImagenInvalidaException extends RuntimeException {

    public ImagenInvalidaException(String mensaje) {
        super(mensaje);
    }
}
