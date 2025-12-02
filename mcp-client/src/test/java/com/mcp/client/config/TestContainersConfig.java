package com.mcp.client.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@TestConfiguration
public class TestContainersConfig {

    private static final PostgreSQLContainer<?> postgresMetaContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("metadb")
            .withUsername("meta")
            .withPassword("metapass")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

    private static final PostgreSQLContainer<?> postgresVectorContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16"))
            .withDatabaseName("vectordb")
            .withUsername("vector")
            .withPassword("vectorpass")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

    // Keycloak sera démarré séparément ou mocké pour les tests
    // Pour simplifier, nous utiliserons un mock OAuth2 ou Keycloak en mode dev sans DB
    private static final GenericContainer<?> keycloakContainer = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:26.0.5"))
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withEnv("KC_HOSTNAME", "localhost")
            .withEnv("KC_PROXY", "edge")
            .withCommand("start-dev")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/health/ready").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));

    private static final MinIOContainer minioContainer = new MinIOContainer(
            DockerImageName.parse("minio/minio:latest"))
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin123")
            .withCommand("server", "--console-address", ":9001", "/data")
            .waitingFor(Wait.forHttp("/minio/health/live").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(30)));

    static {
        postgresMetaContainer.start();
        postgresVectorContainer.start();
        minioContainer.start();
        
        // Keycloak en mode dev (sans DB externe pour simplifier les tests)
        keycloakContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL métadonnées
        registry.add("spring.datasource.url", 
            () -> String.format("jdbc:postgresql://localhost:%d/metadb", postgresMetaContainer.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "meta");
        registry.add("spring.datasource.password", () -> "metapass");

        // PostgreSQL vecteurs
        registry.add("spring.vector.datasource.url", 
            () -> String.format("jdbc:postgresql://localhost:%d/vectordb", postgresVectorContainer.getMappedPort(5432)));
        registry.add("spring.vector.datasource.username", () -> "vector");
        registry.add("spring.vector.datasource.password", () -> "vectorpass");

        // Keycloak
        registry.add("spring.security.oauth2.client.provider.authserver.issuer-uri", 
            () -> String.format("http://localhost:%d/realms/mcp-realm", keycloakContainer.getMappedPort(8080)));
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", 
            () -> String.format("http://localhost:%d/realms/mcp-realm", keycloakContainer.getMappedPort(8080)));

        // MinIO
        registry.add("spring.minio.url", 
            () -> String.format("http://localhost:%d", minioContainer.getMappedPort(9000)));
        registry.add("spring.minio.access-key", () -> "minioadmin");
        registry.add("spring.minio.secret-key", () -> "minioadmin123");
    }

    @Bean
    public static PostgreSQLContainer<?> getPostgresMetaContainer() {
        return postgresMetaContainer;
    }

    @Bean
    public static PostgreSQLContainer<?> getPostgresVectorContainer() {
        return postgresVectorContainer;
    }

    @Bean
    public static GenericContainer<?> getKeycloakContainer() {
        return keycloakContainer;
    }

    @Bean
    public static MinIOContainer getMinioContainer() {
        return minioContainer;
    }
}

