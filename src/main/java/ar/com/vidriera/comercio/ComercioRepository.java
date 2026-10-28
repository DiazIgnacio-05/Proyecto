package ar.com.vidriera.comercio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data genera la implementación a partir del nombre de cada método
 * ({@code findBySlug} → {@code SELECT ... WHERE slug = ?}).
 */
public interface ComercioRepository extends JpaRepository<Comercio, Long> {

    Optional<Comercio> findBySlug(String slug);

    Optional<Comercio> findByDominioPropio(String dominioPropio);

    boolean existsBySlug(String slug);

    boolean existsByDominioPropioAndIdNot(String dominioPropio, Long id);

    List<Comercio> findAllByOrderByNombreAsc();

    /** Si el dominio es el dominio propio de un comercio que no está de baja y tiene ese módulo contratado. */
    @Query("""
            select count(c) > 0 from Comercio c join c.modulos m
            where c.dominioPropio = :dominio and m = :modulo and c.estado <> :baja""")
    boolean existeDominioHabilitado(String dominio, Modulo modulo, EstadoComercio baja);
}
