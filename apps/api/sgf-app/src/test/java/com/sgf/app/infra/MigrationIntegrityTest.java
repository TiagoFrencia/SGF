package com.sgf.app.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.sgf.app.support.PostgresIntegrationTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
class MigrationIntegrityTest extends PostgresIntegrationTestSupport {

    @Autowired
    private Flyway flyway;

    @Test
    void migrationsAreAppliedSuccessfully() {
        assertThat(flyway).isNotNull();
        // Verificar que no haya migraciones pendientes o fallidas
        assertThat(flyway.info().all()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
    }
}
