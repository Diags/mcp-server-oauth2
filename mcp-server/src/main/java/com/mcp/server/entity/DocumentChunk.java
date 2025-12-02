package com.mcp.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    private Long id;

    private String documentId;  // Référence vers DocumentMetadata

    private String content;     // Texte du chunk

    private float[] embedding;  // Vecteur

    private Integer chunkIndex;
    private Integer chunkSize;
}

