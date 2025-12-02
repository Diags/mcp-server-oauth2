package com.mcp.client.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TestUtils {

    /**
     * Crée un document PDF de test avec du contenu
     */
    public static String createTestPdfBase64(String content) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA, 12);
                contentStream.newLineAtOffset(25, 750);
                contentStream.showText(content);
                contentStream.endText();
            }
            
            document.save(baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    /**
     * Crée un document texte de test encodé en Base64
     */
    public static String createTestTextBase64(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Crée un document PDF de test simple avec du texte par défaut
     */
    public static String createDefaultTestPdfBase64() throws IOException {
        String content = "This is a test document for MCP testing. " +
                "It contains some sample text that can be used for vector search testing. " +
                "The document discusses various topics including mathematics, science, and technology.";
        return createTestPdfBase64(content);
    }

    /**
     * Crée un document texte simple avec du contenu par défaut
     */
    public static String createDefaultTestTextBase64() {
        String content = "This is a test text document for MCP testing. " +
                "It contains sample content that can be used for semantic search. " +
                "The text discusses various topics including artificial intelligence and machine learning.";
        return createTestTextBase64(content);
    }

    /**
     * Crée un document avec un contenu spécifique pour les tests de recherche
     */
    public static String createSearchableDocumentBase64(String topic) throws IOException {
        String content = String.format(
            "This document is about %s. " +
            "It provides detailed information on the subject. " +
            "The content includes explanations, examples, and use cases. " +
            "This document can be used for testing semantic search functionality.",
            topic
        );
        return createTestPdfBase64(content);
    }

    /**
     * Nettoie les données de test (peut être utilisé pour nettoyer les bases de données)
     */
    public static void cleanupTestData() {
        // Cette méthode peut être étendue pour nettoyer les données de test
        // Par exemple, supprimer les documents uploadés, vider les tables, etc.
    }
}

