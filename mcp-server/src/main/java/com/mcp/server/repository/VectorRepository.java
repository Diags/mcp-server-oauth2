package com.mcp.server.repository;

import com.mcp.server.entity.DocumentChunk;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class VectorRepository {

    @Qualifier("vectorJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    // Recherche sémantique
    public List<DocumentChunk> semanticSearch(float[] queryVector, int limit) {
        String sql = """
            SELECT id, document_id, content, chunk_index, chunk_size,
                   (embedding <=> ?) as distance
            FROM document_chunks
            ORDER BY embedding <=> ?
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new DocumentChunkRowMapper(),
                new PGvector(queryVector),
                new PGvector(queryVector),
                limit);
    }

    // Insertion
    public void insertChunk(DocumentChunk chunk) {
        String sql = """
            INSERT INTO document_chunks (document_id, content, embedding, chunk_index, chunk_size)
            VALUES (?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                chunk.getDocumentId(),
                chunk.getContent(),
                new PGvector(chunk.getEmbedding()),
                chunk.getChunkIndex(),
                chunk.getChunkSize()
        );
    }

    private static class DocumentChunkRowMapper implements RowMapper<DocumentChunk> {
        @Override
        public DocumentChunk mapRow(ResultSet rs, int rowNum) throws SQLException {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setId(rs.getLong("id"));
            chunk.setDocumentId(rs.getString("document_id"));
            chunk.setContent(rs.getString("content"));
            chunk.setChunkIndex(rs.getInt("chunk_index"));
            chunk.setChunkSize(rs.getInt("chunk_size"));
            
            // Récupérer le vecteur depuis pgvector
            Object embeddingObj = rs.getObject("embedding");
            if (embeddingObj != null) {
                if (embeddingObj instanceof PGvector) {
                    chunk.setEmbedding(((PGvector) embeddingObj).toArray());
                }
            }
            
            return chunk;
        }
    }
}

