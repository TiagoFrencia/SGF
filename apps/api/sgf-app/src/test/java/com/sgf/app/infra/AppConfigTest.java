package com.sgf.app.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.sgf.app.SgfApplication;
import com.sgf.app.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
@Tag("integration")
class AppConfigTest extends PostgresIntegrationTestSupport {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void criticalBeansArePresent() {
        assertThat(context.getBean(SgfApplication.class)).isNotNull();
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        assertThat(activeProfiles).contains("test");
    }
}
