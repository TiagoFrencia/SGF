package com.sgf.core.enterprise;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para estrategia de Sharding.
 */
class ShardingStrategyTest {

    private ShardingStrategy strategy;
    private Map<String, DataSource> shards;

    @BeforeEach
    void setUp() {
        shards = new HashMap<>();
        
        // Crear datasources mock para testing
        shards.put("tenant_001", createMockDataSource("jdbc:postgresql://localhost/tenant001"));
        shards.put("tenant_002", createMockDataSource("jdbc:postgresql://localhost/tenant002"));
        shards.put("date_2024_q1", createMockDataSource("jdbc:postgresql://localhost/archive_q1"));
        shards.put("geo_ba", createMockDataSource("jdbc:postgresql://localhost/buenos_aires"));
        
        strategy = new ShardingStrategy(ShardingStrategy.ShardType.BY_TENANT, shards);
    }

    @Test
    @DisplayName("Debe determinar shard correcto por tenant")
    void shouldDetermineShardByTenant() {
        ShardingStrategy.ShardContext context = ShardingStrategy.ShardContext.forTenant("001");
        
        DataSource dataSource = strategy.determineShard(context);
        
        assertNotNull(dataSource);
        assertEquals("jdbc:postgresql://localhost/tenant001", 
            ((DriverManagerDataSource)dataSource).getUrl());
    }

    @Test
    @DisplayName("Debe lanzar excepción si shard no existe")
    void shouldThrowExceptionIfShardNotFound() {
        ShardingStrategy.ShardContext context = ShardingStrategy.ShardContext.forTenant("999");
        
        assertThrows(ShardingStrategy.ShardNotFoundException.class, () -> 
            strategy.determineShard(context)
        );
    }

    @Test
    @DisplayName("Debe verificar existencia de shard")
    void shouldCheckShardExistence() {
        assertTrue(strategy.hasShard("tenant_001"));
        assertTrue(strategy.hasShard("tenant_002"));
        assertFalse(strategy.hasShard("tenant_999"));
    }

    @Test
    @DisplayName("Debe obtener todos los shards disponibles")
    void shouldGetAllShards() {
        Map<String, DataSource> allShards = strategy.getAllShards();
        
        assertEquals(4, allShards.size());
        assertTrue(allShards.containsKey("tenant_001"));
        assertTrue(allShards.containsKey("date_2024_q1"));
    }

    @Test
    @DisplayName("Debe crear nuevo shard dinámicamente")
    void shouldCreateNewShardDynamically() {
        String newShardKey = "tenant_003";
        String jdbcUrl = "jdbc:postgresql://localhost/tenant003";
        
        DataSource newDataSource = strategy.createShardDataSource(
            newShardKey, jdbcUrl, "user", "password"
        );
        
        assertTrue(strategy.hasShard(newShardKey));
        assertEquals(jdbcUrl, ((DriverManagerDataSource)newDataSource).getUrl());
    }

    @Test
    @DisplayName("Debe generar clave correcta para sharding por fecha")
    void shouldGenerateCorrectKeyForDateSharding() {
        ShardingStrategy dateStrategy = new ShardingStrategy(
            ShardingStrategy.ShardType.BY_DATE, shards
        );
        
        ShardingStrategy.ShardContext context = ShardingStrategy.ShardContext.forDateRange("2024_q2");
        
        // Debería intentar buscar "date_2024_q2" (que no existe en este test)
        assertThrows(ShardingStrategy.ShardNotFoundException.class, () -> 
            dateStrategy.determineShard(context)
        );
    }

    @Test
    @DisplayName("Debe generar clave correcta para sharding geográfico")
    void shouldGenerateCorrectKeyForGeoSharding() {
        ShardingStrategy geoStrategy = new ShardingStrategy(
            ShardingStrategy.ShardType.BY_GEOGRAPHY, shards
        );
        
        ShardingStrategy.ShardContext context = ShardingStrategy.ShardContext.forRegion("ba");
        
        DataSource dataSource = geoStrategy.determineShard(context);
        
        assertNotNull(dataSource);
        assertEquals("jdbc:postgresql://localhost/buenos_aires", 
            ((DriverManagerDataSource)dataSource).getUrl());
    }

    @Test
    @DisplayName("Debe soportar estrategia híbrida tenant+fecha")
    void shouldSupportHybridStrategy() {
        Map<String, DataSource> hybridShards = new HashMap<>();
        hybridShards.put("001_2024_q1", createMockDataSource("jdbc:postgresql://localhost/hybrid1"));
        
        ShardingStrategy hybridStrategy = new ShardingStrategy(
            ShardingStrategy.ShardType.HYBRID, hybridShards
        );
        
        ShardingStrategy.ShardContext context = new ShardingStrategy.ShardContext(
            "001", "2024_q1", null
        );
        
        DataSource dataSource = hybridStrategy.determineShard(context);
        
        assertNotNull(dataSource);
        assertEquals("jdbc:postgresql://localhost/hybrid1", 
            ((DriverManagerDataSource)dataSource).getUrl());
    }

    @Test
    @DisplayName("Debe retornar copia inmutable de shards")
    void shouldReturnImmutableCopyOfShards() {
        Map<String, DataSource> allShards = strategy.getAllShards();
        
        assertThrows(UnsupportedOperationException.class, () -> 
            allShards.put("new_shard", createMockDataSource("jdbc:postgresql://localhost/new"))
        );
    }

    private DataSource createMockDataSource(String url) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(url);
        ds.setUsername("test");
        ds.setPassword("test");
        return ds;
    }
}
