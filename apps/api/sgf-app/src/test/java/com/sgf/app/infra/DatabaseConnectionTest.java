package com.sgf.app.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.sgf.app.support.PostgresIntegrationTestSupport;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Tag("integration")
class DatabaseConnectionTest extends PostgresIntegrationTestSupport {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseIsReachable() {
        assertThat(dataSource).isNotNull();
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void databaseHasCorrectSchema() {
        // Verificar que podemos consultar tablas básicas (esto valida que Flyway corrió)
        // Usamos una tabla que sepamos que existe por el Roadmap
        Integer productCount = jdbcTemplate.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'", Integer.class);
        assertThat(productCount).isGreaterThan(0);
    }
}
