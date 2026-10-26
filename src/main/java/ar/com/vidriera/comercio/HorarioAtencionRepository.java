package ar.com.vidriera.comercio;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Todas las consultas se filtran por el comercio actual ({@code @TenantId}).
 * El orden por día se hace en {@link CalendarioAtencion}: en la base el día se guarda como texto
 * ('MONDAY', ...) y ordenarlo ahí daría orden alfabético, no semanal.
 */
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {
}
