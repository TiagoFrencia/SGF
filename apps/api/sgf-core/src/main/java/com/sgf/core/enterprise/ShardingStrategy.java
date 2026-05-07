package com.sgf.core.enterprise;

import org.hibernate.ConnectionProvider;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Estrategia de Sharding para distribución horizontal de datos.
 * 
 * Casos de uso:
 * - Farmacias con >100,000 transacciones/mes
 * - Multi-sucursal con necesidad de aislamiento
 * - Archive de datos históricos en shards fríos
 * 
 * @author SGF Enterprise Team
 */
public class ShardingStrategy {

    /**
     * Tipos de estrategia de sharding soportadas.
     */
    public enum ShardType {
        BY_TENANT,      // Un shard por cliente (farmacia)
        BY_DATE,        // Shard por rango de fechas (histórico)
        BY_GEOGRAPHY,   // Shard por región/provincia
        HYBRID          // Combinación de estrategias
    }

    private final ShardType type;
    private final Map<String, DataSource> shards;

    public ShardingStrategy(ShardType type, Map<String, DataSource> shards) {
        this.type = type;
        this.shards = shards;
    }

    /**
     * Determina el shard apropiado para una operación dada.
     * 
     * @param context Contexto de la operación (tenantId, fecha, región)
     * @return DataSource del shard seleccionado
     */
    public DataSource determineShard(ShardContext context) {
        String shardKey = generateShardKey(context);
        
        DataSource dataSource = shards.get(shardKey);
        if (dataSource == null) {
            throw new ShardNotFoundException("Shard no encontrado para key: " + shardKey);
        }
        
        return dataSource;
    }

    /**
     * Genera la clave de shard según la estrategia configurada.
     */
    private String generateShardKey(ShardContext context) {
        return switch (type) {
            case BY_TENANT -> "tenant_" + context.getTenantId();
            case BY_DATE -> "date_" + context.getDateRange();
            case BY_GEOGRAPHY -> "geo_" + context.getRegion();
            case HYBRID -> context.getTenantId() + "_" + context.getDateRange();
        };
    }

    /**
     * Obtiene todos los shards disponibles.
     */
    public Map<String, DataSource> getAllShards() {
        return Map.copyOf(shards);
    }

    /**
     * Verifica si un shard específico existe.
     */
    public boolean hasShard(String shardKey) {
        return shards.containsKey(shardKey);
    }

    /**
     * Crea un DataSource para un nuevo shard.
     */
    public DataSource createShardDataSource(String shardKey, String jdbcUrl, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        shards.put(shardKey, dataSource);
        return dataSource;
    }

    /**
     * Contexto para determinación de shard.
     */
    public static class ShardContext {
        private final String tenantId;
        private final String dateRange;
        private final String region;

        public ShardContext(String tenantId, String dateRange, String region) {
            this.tenantId = tenantId;
            this.dateRange = dateRange;
            this.region = region;
        }

        public String getTenantId() { return tenantId; }
        public String getDateRange() { return dateRange; }
        public String getRegion() { return region; }

        public static ShardContext forTenant(String tenantId) {
            return new ShardContext(tenantId, null, null);
        }

        public static ShardContext forDateRange(String dateRange) {
            return new ShardContext(null, dateRange, null);
        }

        public static ShardContext forRegion(String region) {
            return new ShardContext(null, null, region);
        }
    }

    /**
     * Excepción cuando no se encuentra un shard.
     */
    public static class ShardNotFoundException extends RuntimeException {
        public ShardNotFoundException(String message) {
            super(message);
        }
    }
}
