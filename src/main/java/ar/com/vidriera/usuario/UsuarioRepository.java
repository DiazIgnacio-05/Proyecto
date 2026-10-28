package ar.com.vidriera.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByRol(Rol rol);

    boolean existsByEmail(String email);

    /** Usuarios de un comercio (para el super-admin). */
    List<Usuario> findByComercioIdOrderByIdAsc(Long comercioId);
}
