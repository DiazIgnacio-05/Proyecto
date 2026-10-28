package ar.com.vidriera.comercio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
