package com.mcp.server.repository;

import com.mcp.server.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {
    List<DocumentMetadata> findByUploadedBy(String username);
    List<DocumentMetadata> findByTagsContaining(String tag);
    Optional<DocumentMetadata> findByDocumentId(String documentId);
}

