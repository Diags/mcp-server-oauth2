package com.mcp.client.controller;

import com.mcp.client.config.TestContainersConfig;
import com.mcp.client.util.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class ChatApiEndToEndTest {

    @Autowired
    private WebTestClient.Builder webTestClientBuilder;

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        webTestClient = webTestClientBuilder
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @DynamicPropertySource
    static void configureMcpServerProperties(DynamicPropertyRegistry registry) {
        // Configuration du serveur MCP si démarré via Testcontainers
        // Pour l'instant, nous utilisons localhost:8080 par défaut
        registry.add("mcp.server.port", () -> "8080");
    }

    @Test
    void testAskWithMathTool() {
        // Teste une question mathématique qui devrait déclencher un outil MCP
        String question = "What is 5 plus 3?";
        
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/chat/ask")
                        .queryParam("question", question)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response).isNotEmpty();
                    // La réponse devrait contenir le résultat du calcul
                    assertThat(response.toLowerCase()).containsAnyOf("8", "eight");
                });
    }

    @Test
    void testAskWithDocumentUpload() throws Exception {
        // Teste l'upload d'un document via une question au LLM
        String pdfBase64 = TestUtils.createDefaultTestPdfBase64();
        String question = String.format(
            "Please upload this document with filename 'test.pdf' and tags 'test,document'. " +
            "The base64 content is: %s", pdfBase64
        );

        // Note: Ce test nécessite que le LLM comprenne comment appeler l'outil uploadDocument
        // Dans un vrai scénario, vous pourriez mock l'API OpenAI ou utiliser un LLM de test
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/chat/ask")
                        .queryParam("question", question)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                });
    }

    @Test
    void testAskWithDocumentSearch() {
        // Teste la recherche de documents via une question au LLM
        String question = "Search for documents about artificial intelligence";

        // Note: Ce test nécessite que des documents soient déjà uploadés
        // et que le LLM comprenne comment appeler l'outil searchDocuments
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/chat/ask")
                        .queryParam("question", question)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                });
    }

    @Test
    void testOAuth2AuthenticationFlow() {
        // Teste que l'authentification OAuth2 fonctionne
        // Le client devrait automatiquement obtenir un token et l'utiliser
        String question = "What is 2 multiplied by 4?";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/chat/ask")
                        .queryParam("question", question)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    // Si l'authentification échoue, nous aurions une erreur 401 ou 403
                });
    }

    @Test
    void testMultipleToolsInOneRequest() {
        // Teste l'utilisation de plusieurs outils dans une seule requête
        String question = "Calculate 10 plus 5, then multiply the result by 2";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/chat/ask")
                        .queryParam("question", question)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    // La réponse devrait contenir le résultat final (30)
                    assertThat(response.toLowerCase()).containsAnyOf("30", "thirty");
                });
    }

    @Test
    void testEndpointExists() {
        // Test simple pour vérifier que l'endpoint existe
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/chat/ask")
                        .queryParam("question", "Hello")
                        .build())
                .exchange()
                .expectStatus().isOk();
    }
}

