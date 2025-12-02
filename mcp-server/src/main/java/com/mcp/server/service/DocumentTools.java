package com.mcp.server.service;

import com.mcp.server.dto.DocumentSearchResult;
import com.mcp.server.dto.DocumentUploadResponse;
import com.mcp.server.entity.DocumentChunk;
import com.mcp.server.entity.DocumentMetadata;
import com.mcp.server.repository.DocumentMetadataRepository;
import com.mcp.server.repository.VectorRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTools {

    private final DocumentMetadataRepository metadataRepo;
    private final VectorRepository vectorRepo;
    private final MinioClient minioClient;
    private final EmbeddingModel embeddingModel;

    // --- Upload tool ---
    @McpTool(description = "Upload a document and store it with metadata")
    @PreAuthorize("hasAuthority('SCOPE_mcp:write')")
    public DocumentUploadResponse uploadDocument(
            @McpToolParam(description = "Base64 encoded file content") String base64Content,
            @McpToolParam(description = "Original filename") String filename,
            @McpToolParam(description = "Tags (comma-separated)", required = false) String tags) {

        try {
            // 1. Sauvegarder dans MinIO
            String documentId = UUID.randomUUID().toString();
            byte[] content = java.util.Base64.getDecoder().decode(base64Content);
            String storagePath = saveToMinIO(documentId, filename, content);

            // 2. Extraire texte (ex: PDFBox pour PDF)
            String extractedText = extractText(content, filename);

            // 3. Créer métadonnées
            DocumentMetadata metadata = new DocumentMetadata();
            metadata.setDocumentId(documentId);
            metadata.setTitle(filename);
            metadata.setFileType(getExtension(filename));
            metadata.setFileSize((long) content.length);
            metadata.setStoragePath(storagePath);
            metadata.setUploadedAt(LocalDateTime.now());
            metadata.setUploadedBy(getCurrentUser());
            if (tags != null && !tags.isEmpty()) {
                metadata.setTags(Arrays.asList(tags.split(",")));
            } else {
                metadata.setTags(new ArrayList<>());
            }
            metadataRepo.save(metadata);

            // 4. Chunker et vectoriser
            List<String> chunks = chunkText(extractedText, 1000); // 1000 mots
            for (int i = 0; i < chunks.size(); i++) {
                float[] embedding = embeddingModel.embed(chunks.get(i));
                
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setContent(chunks.get(i));
                chunk.setEmbedding(embedding);
                chunk.setChunkIndex(i);
                chunk.setChunkSize(chunks.get(i).length());
                vectorRepo.insertChunk(chunk);
            }

            DocumentUploadResponse response = new DocumentUploadResponse();
            response.setDocumentId(documentId);
            response.setMessage("Uploaded successfully");
            return response;
        } catch (Exception e) {
            log.error("Error uploading document", e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    // --- Search tool ---
    @McpTool(description = "Search documents by semantic similarity")
    @PreAuthorize("hasAuthority('SCOPE_mcp:read')")
    public List<DocumentSearchResult> searchDocuments(
            @McpToolParam(description = "Search query") String query,
            @McpToolParam(description = "Max results", required = false) Integer limit) {

        try {
            // 1. Vectoriser la requête
            float[] queryVector = embeddingModel.embed(query);

            // 2. Recherche sémantique
            int maxResults = limit != null ? limit : 5;
            List<DocumentChunk> chunks = vectorRepo.semanticSearch(queryVector, maxResults);

            // 3. Récupérer métadonnées
            return chunks.stream().map(chunk -> {
                DocumentMetadata meta = metadataRepo.findByDocumentId(chunk.getDocumentId())
                        .orElseThrow(() -> new RuntimeException("Metadata not found for document: " + chunk.getDocumentId()));
                return new DocumentSearchResult(
                        meta.getTitle(),
                        meta.getAuthor(),
                        chunk.getContent(),
                        meta.getTags(),
                        meta.getUploadedAt()
                );
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching documents", e);
            throw new RuntimeException("Failed to search documents: " + e.getMessage(), e);
        }
    }

    // Helpers
    private String saveToMinIO(String docId, String filename, byte[] content) {
        try {
            String bucketName = "documents";
            // Créer le bucket s'il n'existe pas
            try {
                if (!minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(bucketName).build())) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(bucketName).build());
                    log.info("Bucket '{}' créé", bucketName);
                }
            } catch (Exception e) {
                log.warn("Erreur lors de la vérification/création du bucket: {}", e.getMessage());
            }
            
            String objectPath = docId + "/" + filename;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType("application/octet-stream")
                            .build()
            );
            return "documents/" + objectPath;
        } catch (Exception e) {
            log.error("MinIO upload failed", e);
            throw new RuntimeException("MinIO upload failed: " + e.getMessage(), e);
        }
    }

    private String extractText(byte[] content, String filename) {
        try {
            String extension = getExtension(filename).toLowerCase();
            if ("pdf".equals(extension)) {
                try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(content)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if ("txt".equals(extension)) {
                return new String(content);
            } else {
                log.warn("Unsupported file type: {}, returning empty text", extension);
                return "";
            }
        } catch (Exception e) {
            log.error("Error extracting text from file", e);
            return "";
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        StringBuilder chunk = new StringBuilder();
        int wordCount = 0;
        
        for (String word : words) {
            if (wordCount >= chunkSize) {
                chunks.add(chunk.toString().trim());
                chunk = new StringBuilder();
                wordCount = 0;
            }
            chunk.append(word).append(" ");
            wordCount++;
        }
        
        if (chunk.length() > 0) {
            chunks.add(chunk.toString().trim());
        }
        
        return chunks.isEmpty() ? List.of(text) : chunks;
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}

