package ar.com.vidriera.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.TenantId;

/**
 * Clase base de toda entidad que pertenece a un comercio.
 *
 * <p>{@code @TenantId} hace que Hibernate filtre y complete {@code comercio_id} automáticamente
 * según {@link TenantContext}. Por eso no hay setter: el comercio de una fila nunca se elige a mano.
 * Regla del proyecto: no usar consultas SQL nativas sobre estas tablas (el filtro no se aplica).
 */
@MappedSuperclass
public abstract class EntidadDelComercio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "comercio_id", nullable = false, updatable = false)
    private Long comercioId;

    public Long getId() {
        return id;
    }

    public Long getComercioId() {
        return comercioId;
    }
}
