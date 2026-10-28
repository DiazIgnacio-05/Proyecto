package ar.com.vidriera.superadmin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ComercioNoEncontradoException extends RuntimeException {

    public ComercioNoEncontradoException(Long id) {
        super("Comercio no encontrado: " + id);
    }
}
