package ar.com.vidriera.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void limpiar() {
        TenantContext.limpiar();
    }

    @Test
    void sinComercioHibernateRecibeNinguno() {
        assertThat(TenantContext.actual()).isEmpty();
        assertThat(TenantContext.paraHibernate()).isEqualTo(TenantContext.NINGUNO);
    }

    @Test
    void ejecutarComoRestauraElValorAnterior() {
        TenantContext.establecer(1L);
        Long dentro = TenantContext.ejecutarComo(2L, () -> TenantContext.actual().orElseThrow());
        assertThat(dentro).isEqualTo(2L);
        assertThat(TenantContext.actual()).contains(1L);
    }

    @Test
    void ejecutarComoRestauraAunAnteUnaExcepcion() {
        assertThatThrownBy(() -> TenantContext.ejecutarComo(5L, () -> {
            throw new IllegalStateException("falla");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.actual()).isEmpty();
    }

    @Test
    void modoSistemaUsaElTenantRaiz() {
        long valor = TenantContext.ejecutarComoSistema(TenantContext::paraHibernate);
        assertThat(new ResolutorTenantHibernate().isRoot(valor)).isTrue();
        assertThat(new ResolutorTenantHibernate().isRoot(TenantContext.NINGUNO)).isFalse();
    }

    @Test
    void rechazaIdsInvalidos() {
        assertThatThrownBy(() -> TenantContext.establecer(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TenantContext.establecer(-1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
