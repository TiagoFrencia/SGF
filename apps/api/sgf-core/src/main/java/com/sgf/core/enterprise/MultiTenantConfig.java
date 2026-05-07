package com.sgf.core.enterprise;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuración Multi-Tenant para aislamiento de datos por farmacia/cliente.
 * 
 * Estrategia: Discriminator column (tenant_id) en todas las tablas.
 * Alternativas futuras: Schema-per-tenant o Database-per-tenant.
 * 
 * @author SGF Enterprise Team
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.sgf")
@EnableTransactionManagement
public class MultiTenantConfig {

    /**
     * Identificador del tenant actual en contexto de thread.
     * Se establece vía interceptor HTTP o evento de sistema.
     */
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Establece el tenant para la operación actual.
     * @param tenantId Identificador único del tenant (CUIT, UUID)
     */
    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID no puede ser nulo o vacío");
        }
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Obtiene el tenant actual.
     * @return Tenant ID o null si no está definido
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Limpia el tenant del contexto actual (importante para thread pools).
     */
    public static void clearTenantId() {
        CURRENT_TENANT.remove();
    }

    /**
     * Verifica si hay un tenant definido en el contexto actual.
     * @return true si hay tenant activo
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}
