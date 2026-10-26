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
}
