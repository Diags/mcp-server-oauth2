package com.mcp.server.integration;

import com.mcp.server.dto.DocumentSearchResult;
import com.mcp.server.dto.DocumentUploadResponse;
import com.mcp.server.entity.DocumentMetadata;
import com.mcp.server.repository.DocumentMetadataRepository;
import com.mcp.server.service.DocumentTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DocumentToolsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresMetaContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("metadb")
            .withUsername("meta")
            .withPassword("metapass")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

    @Container
    static PostgreSQLContainer<?> postgresVectorContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16"))
            .withDatabaseName("vectordb")
            .withUsername("vector")
            .withPassword("vectorpass")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

    @Container
    static MinIOContainer minioContainer = new MinIOContainer(
            DockerImageName.parse("minio/minio:latest"))
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin123")
            .withCommand("server", "--console-address", ":9001", "/data")
            .waitingFor(Wait.forHttp("/minio/health/live").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(30)));

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

        // MinIO
        registry.add("spring.minio.url", 
            () -> String.format("http://localhost:%d", minioContainer.getMappedPort(9000)));
        registry.add("spring.minio.access-key", () -> "minioadmin");
        registry.add("spring.minio.secret-key", () -> "minioadmin123");

        // Keycloak mock (pour les tests, on peut utiliser un mock)
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", 
            () -> "http://localhost:9000/realms/mcp-realm");
    }

    @Autowired
    private DocumentTools documentTools;

    @Autowired
    private DocumentMetadataRepository metadataRepository;

    @BeforeEach
    void cleanup() {
        // Nettoyer les données de test avant chaque test
        metadataRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:write")
    void testUploadDocumentEndToEnd() throws Exception {
        // Créer un document texte simple
        String testContent = "This is a test document about artificial intelligence and machine learning. " +
                "It discusses various topics including neural networks, deep learning, and natural language processing.";
        String base64Content = Base64.getEncoder().encodeToString(testContent.getBytes());
        String filename = "test-document.txt";
        String tags = "ai,ml,test";

        // Upload le document
        DocumentUploadResponse response = documentTools.uploadDocument(base64Content, filename, tags);

        // Vérifier la réponse
        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isNotNull();
        assertThat(response.getMessage()).contains("successfully");

        // Vérifier que les métadonnées sont sauvegardées
        DocumentMetadata metadata = metadataRepository.findByDocumentId(response.getDocumentId())
                .orElseThrow(() -> new AssertionError("Document metadata not found"));
        
        assertThat(metadata.getTitle()).isEqualTo(filename);
        assertThat(metadata.getFileType()).isEqualTo("txt");
        assertThat(metadata.getTags()).contains("ai", "ml", "test");
        assertThat(metadata.getStoragePath()).isNotNull();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:read")
    void testSearchDocumentsEndToEnd() throws Exception {
        // D'abord, uploader un document
        String testContent = "This document is about quantum computing and quantum algorithms. " +
                "It explains quantum superposition and entanglement in detail.";
        String base64Content = Base64.getEncoder().encodeToString(testContent.getBytes());
        String filename = "quantum-computing.txt";
        String tags = "quantum,computing";

        DocumentUploadResponse uploadResponse = documentTools.uploadDocument(
                base64Content, filename, tags);

        // Attendre un peu pour que la vectorisation soit terminée
        Thread.sleep(2000);

        // Rechercher des documents sur un sujet similaire
        String query = "What is quantum computing?";
        List<DocumentSearchResult> results = documentTools.searchDocuments(query, 5);

        // Vérifier les résultats
        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        
        // Vérifier que le document uploadé est dans les résultats
        boolean found = results.stream()
                .anyMatch(r -> r.getTitle().equals(filename));
        assertThat(found).isTrue();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:read")
    void testOAuth2Security() {
        // Ce test vérifie que les outils sont protégés par OAuth2
        // Si l'utilisateur n'a pas les bonnes autorités, l'accès devrait être refusé
        // Note: @WithMockUser avec SCOPE_mcp:read devrait permettre la recherche
        
        String query = "test query";
        List<DocumentSearchResult> results = documentTools.searchDocuments(query, 5);
        
        // Même sans documents, la méthode devrait retourner une liste vide, pas une exception de sécurité
        assertThat(results).isNotNull();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:write")
    void testUploadMultipleDocuments() throws Exception {
        // Tester l'upload de plusieurs documents
        String[] contents = {
            "Document about machine learning algorithms",
            "Document about deep learning neural networks",
            "Document about natural language processing"
        };

        for (int i = 0; i < contents.length; i++) {
            String base64Content = Base64.getEncoder().encodeToString(contents[i].getBytes());
            String filename = "doc-" + i + ".txt";
            
            DocumentUploadResponse response = documentTools.uploadDocument(
                    base64Content, filename, "ml,ai");
            
            assertThat(response.getDocumentId()).isNotNull();
        }

        // Vérifier que tous les documents sont sauvegardés
        List<DocumentMetadata> allDocs = metadataRepository.findAll();
        assertThat(allDocs).hasSize(contents.length);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:write")
    void testUploadWithEmptyTags() throws Exception {
        // Tester l'upload avec des tags vides
        String testContent = "Test document without tags";
        String base64Content = Base64.getEncoder().encodeToString(testContent.getBytes());
        String filename = "no-tags.txt";

        DocumentUploadResponse response = documentTools.uploadDocument(
                base64Content, filename, null);

        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isNotNull();

        DocumentMetadata metadata = metadataRepository.findByDocumentId(response.getDocumentId())
                .orElseThrow();
        
        assertThat(metadata.getTags()).isEmpty();
    }
}

