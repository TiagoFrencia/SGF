package com.sgf.app.diagnostics;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.VersionCmd;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class TestcontainersProbe {

    private TestcontainersProbe() {
    }

    public static void main(String[] args) {
        String dockerHost = System.getenv().getOrDefault("DOCKER_HOST", "<unset>");
        System.out.println("DOCKER_HOST=" + dockerHost);

        DockerClient client = DockerClientFactory.instance().client();
        try (VersionCmd versionCmd = client.versionCmd()) {
            var version = versionCmd.exec();
            System.out.println("Docker version=" + version.getVersion() + " api=" + version.getApiVersion());
        }

        try (GenericContainer<?> alpine = new GenericContainer<>(DockerImageName.parse("alpine:3.20"))
                .withCommand("sh", "-c", "echo testcontainers-probe && sleep 1")) {
            alpine.start();
            System.out.println("Alpine container started with id=" + alpine.getContainerId());
        }

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("sgf_probe")
                .withUsername("sgf")
                .withPassword("sgf")) {
            postgres.start();
            System.out.println("PostgreSQL JDBC URL=" + postgres.getJdbcUrl());
        }

        System.out.println("Testcontainers probe completed successfully.");
    }
}
