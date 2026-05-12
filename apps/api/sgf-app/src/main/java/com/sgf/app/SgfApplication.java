package com.sgf.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sgf")
@ConfigurationPropertiesScan(basePackages = "com.sgf")
@EntityScan(basePackages = "com.sgf")
@EnableJpaRepositories(basePackages = "com.sgf")
public class SgfApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgfApplication.class, args);
    }
}
