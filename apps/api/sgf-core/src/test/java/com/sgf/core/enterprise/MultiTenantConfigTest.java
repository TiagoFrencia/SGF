package com.sgf.core.enterprise;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para configuración Multi-Tenant.
 */
class MultiTenantConfigTest {

    @BeforeEach
    void setUp() {
        MultiTenantConfig.clearTenantId();
    }

    @Test
    @DisplayName("Debe establecer y obtener tenant ID correctamente")
    void shouldSetAndGetTenantId() {
        String tenantId = "CUIT-30-12345678-9";
        
        MultiTenantConfig.setTenantId(tenantId);
        String retrieved = MultiTenantConfig.getTenantId();
        
        assertEquals(tenantId, retrieved);
    }

    @Test
    @DisplayName("Debe rechazar tenant ID nulo")
    void shouldRejectNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> 
            MultiTenantConfig.setTenantId(null)
        );
    }

    @Test
    @DisplayName("Debe rechazar tenant ID vacío")
    void shouldRejectEmptyTenantId() {
        assertThrows(IllegalArgumentException.class, () -> 
            MultiTenantConfig.setTenantId("")
        );
    }

    @Test
    @DisplayName("Debe rechazar tenant ID con solo espacios")
    void shouldRejectBlankTenantId() {
        assertThrows(IllegalArgumentException.class, () -> 
            MultiTenantConfig.setTenantId("   ")
        );
    }

    @Test
    @DisplayName("Debe limpiar tenant ID del contexto")
    void shouldClearTenantId() {
        MultiTenantConfig.setTenantId("tenant-123");
        assertNotNull(MultiTenantConfig.getTenantId());
        
        MultiTenantConfig.clearTenantId();
        assertNull(MultiTenantConfig.getTenantId());
    }

    @Test
    @DisplayName("Debe verificar existencia de tenant activo")
    void shouldCheckHasTenant() {
        assertFalse(MultiTenantConfig.hasTenant());
        
        MultiTenantConfig.setTenantId("tenant-456");
        assertTrue(MultiTenantConfig.hasTenant());
        
        MultiTenantConfig.clearTenantId();
        assertFalse(MultiTenantConfig.hasTenant());
    }

    @Test
    @DisplayName("Debe aislar tenants entre threads diferentes")
    void shouldIsolateTenantsBetweenThreads() throws InterruptedException {
        String[] threadResults = new String[2];
        
        Thread thread1 = new Thread(() -> {
            MultiTenantConfig.setTenantId("thread-1-tenant");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            threadResults[0] = MultiTenantConfig.getTenantId();
            MultiTenantConfig.clearTenantId();
        });
        
        Thread thread2 = new Thread(() -> {
            MultiTenantConfig.setTenantId("thread-2-tenant");
            try { Thread.sleep(50); } catch (InterruptedException e) {}
            threadResults[1] = MultiTenantConfig.getTenantId();
            MultiTenantConfig.clearTenantId();
        });
        
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        
        assertEquals("thread-1-tenant", threadResults[0]);
        assertEquals("thread-2-tenant", threadResults[1]);
    }
}
