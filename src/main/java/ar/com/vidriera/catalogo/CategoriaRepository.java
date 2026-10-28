package ar.com.vidriera.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Todas las consultas se filtran por el comercio actual ({@code @TenantId}). */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByOrdenAscNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
