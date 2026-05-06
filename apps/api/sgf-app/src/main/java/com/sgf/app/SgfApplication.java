package com.sgf.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.sgf")
@EnableConfigurationProperties
public class SgfApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgfApplication.class, args);
    }
}