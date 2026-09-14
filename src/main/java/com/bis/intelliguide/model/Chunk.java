package com.bis.intelliguide.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single retrieval-eligible chunk of text, embedded and stored inline
 * within its parent document (Standard / CertificationScheme / BisService).
 * Indexed via a MongoDB Atlas Vector Search index on `chunks.embedding`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
    private String text;
    private List<Double> embedding;
    private String clauseRef;
}
