package ar.com.vidriera.tenant;

import java.util.Map;

import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Le dice a Hibernate cuál es el comercio actual cada vez que abre una sesión.
 *
 * <p>Con ese valor Hibernate agrega {@code WHERE comercio_id = ?} a toda consulta sobre entidades
 * con {@code @TenantId} y completa la columna al insertar. Si no hay comercio en el contexto usa
 * {@link TenantContext#NINGUNO}, de modo que por defecto no se ve ningún dato (falla cerrado).
 */
@Component
public class ResolutorTenantHibernate
        implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.paraHibernate();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    /** El tenant "raíz" no tiene filtro: solo se usa en {@link TenantContext#ejecutarComoSistema}. */
    @Override
    public boolean isRoot(Long tenantId) {
        return tenantId != null && tenantId == TenantContext.SISTEMA;
    }

    @Override
    public void customize(Map<String, Object> propiedadesHibernate) {
        propiedadesHibernate.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
