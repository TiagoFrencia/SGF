package com.sgf.api;

import com.sgf.api.config.JwtProperties;
import com.sgf.modules.integrations.adesfa.service.AdesfaProperties;
import com.sgf.modules.integrations.afip.service.AfipProperties;
import com.sgf.modules.integrations.anmat.service.AnmatProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sgf")
@EnableConfigurationProperties({JwtProperties.class, AfipProperties.class, AnmatProperties.class, AdesfaProperties.class})
@EnableJpaRepositories(basePackages = "com.sgf")
@EntityScan(basePackages = "com.sgf")
public class SgfApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgfApiApplication.class, args);
    }
}
