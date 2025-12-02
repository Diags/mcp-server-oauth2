package com.mcp.server.config;

import com.zaxxer.hikari.HikariDataSource;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@EnableJpaRepositories(basePackages = "com.mcp.server.repository")
@EnableTransactionManagement
@Slf4j
public class VectorDatabaseConfig {

    @Value("${spring.vector.datasource.url}")
    private String vectorDbUrl;

    @Value("${spring.vector.datasource.username}")
    private String vectorDbUsername;

    @Value("${spring.vector.datasource.password}")
    private String vectorDbPassword;

    @PostConstruct
    public void init() {
        // Créer l'extension pgvector et la table document_chunks
        try (Connection conn = DriverManager.getConnection(
                vectorDbUrl, vectorDbUsername, vectorDbPassword)) {
            try (Statement stmt = conn.createStatement()) {
                // Créer l'extension pgvector
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
                log.info("Extension pgvector créée avec succès");
                
                // Créer la table document_chunks si elle n'existe pas
                String createTableSql = """
                    CREATE TABLE IF NOT EXISTS document_chunks (
                        id BIGSERIAL PRIMARY KEY,
                        document_id VARCHAR(255) NOT NULL,
                        content TEXT NOT NULL,
                        embedding vector(1536),
                        chunk_index INTEGER NOT NULL,
                        chunk_size INTEGER NOT NULL
                    )
                    """;
                stmt.execute(createTableSql);
                log.info("Table document_chunks créée ou déjà existante");
                
                // Créer un index pour améliorer les performances de recherche
                try {
                    stmt.execute("CREATE INDEX IF NOT EXISTS document_chunks_embedding_idx ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)");
                    log.info("Index vectoriel créé ou déjà existant");
                } catch (SQLException e) {
                    log.warn("Impossible de créer l'index vectoriel (peut-être déjà existant): {}", e.getMessage());
                }
            }
        } catch (SQLException e) {
            log.warn("PostgreSQL vector non disponible, utilisation d'un mock: {}", e.getMessage());
        }
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties metadataDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource metadataDataSource(@Qualifier("metadataDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("spring.vector.datasource")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("vectorDataSource")
    public DataSource vectorDataSource(@Qualifier("vectorDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Qualifier("vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public EmbeddingModel embeddingModel(@Value("${spring.ai.openai.api-key}") String apiKey) {
        var openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
        return new OpenAiEmbeddingModel(openAiApi);
    }

    @Bean
    public MinioClient minioClient(
            @Value("${spring.minio.url}") String url,
            @Value("${spring.minio.access-key}") String accessKey,
            @Value("${spring.minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}

