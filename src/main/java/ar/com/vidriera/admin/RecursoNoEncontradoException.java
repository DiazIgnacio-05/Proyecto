package ar.com.vidriera.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El id pedido no existe para este comercio. Es la misma respuesta (404) si no existe en absoluto
 * o si es de otro comercio: no le damos pistas a nadie sobre datos ajenos.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso) {
        super(recurso + " no encontrado");
    }
}
