package ar.com.vidriera.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Todas las consultas se filtran por el comercio actual ({@code @TenantId}). */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByOrderByOrdenAscNombreAsc();

    /** Trae las variantes en la misma consulta ("fetch join") para poder usarlas fuera de la transacción. */
    @Query("select distinct p from Producto p left join fetch p.variantes order by p.orden, p.nombre")
    List<Producto> findAllConVariantes();

    boolean existsByCategoriaId(Long categoriaId);

    /** Cantidad de productos por categoría: filas {@code [categoriaId, cantidad]}. */
    @Query("select p.categoria.id, count(p) from Producto p group by p.categoria.id")
    List<Object[]> contarPorCategoria();

    long countByDisponibleFalse();

    /**
     * Cantidad de productos de CADA comercio: filas {@code [comercioId, cantidad]}. Solo tiene sentido
     * dentro de {@code TenantContext.ejecutarComoSistema} (sin filtro de tenant); con un comercio en
     * el contexto devolvería solo el suyo.
     */
    @Query("select p.comercioId, count(p) from Producto p group by p.comercioId")
    List<Object[]> contarPorComercio();
}
