package com.mcp.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String documentId;  // UUID

    private String title;
    private String author;
    private String fileType;    // pdf, txt, docx
    private Long fileSize;      // bytes
    private String storagePath; // path MinIO/S3

    @Column(columnDefinition = "TEXT")
    private String summary;     // Résumé généré

    @ElementCollection
    private List<String> tags = new ArrayList<>();

    private LocalDateTime uploadedAt;
    private String uploadedBy;  // Keycloak username
}

