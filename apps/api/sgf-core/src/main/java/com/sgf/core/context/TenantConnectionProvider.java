package com.sgf.core.context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Datasource wrapper that sets the PostgreSQL session variable `app.tenant_id`
 * on every acquired connection, enabling RLS policies to filter data automatically.
 *
 * This is transparent to the application layer — JPA and JDBC code need no changes.
 */
public class TenantConnectionProvider extends DelegatingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantConnectionProvider.class);

    public TenantConnectionProvider(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        applyTenantContext(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        applyTenantContext(conn);
        return conn;
    }

    private void applyTenantContext(Connection conn) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET app.tenant_id = '" + tenantId.replace("'", "''") + "'");
                log.trace("Set app.tenant_id = {} on connection", tenantId);
            } catch (SQLException e) {
                log.error("Failed to set tenant context on connection: {}", e.getMessage());
            }
        }
    }
}
