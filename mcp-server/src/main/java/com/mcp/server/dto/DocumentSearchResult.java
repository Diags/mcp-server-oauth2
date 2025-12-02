package com.mcp.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResult {
    private String title;
    private String author;
    private String content;
    private List<String> tags;
    private LocalDateTime uploadedAt;
}

