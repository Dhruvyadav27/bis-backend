package com.bis.intelliguide.service.rag;

import com.bis.intelliguide.model.Chunk;
import com.bis.intelliguide.model.RagChunk;
import com.bis.intelliguide.repository.RagChunkRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private final EmbeddingModel embeddingModel;
    private final RagChunkRepository ragChunkRepository;

    public List<Chunk> ingest(String rawText, String clauseRefPrefix) {
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        Document document = Document.from(rawText);
        List<TextSegment> segments = splitter.split(document);

        List<Chunk> chunks = new ArrayList<>();
        int i = 1;
        for (TextSegment segment : segments) {
            List<Double> embeddingVector = safeEmbed(segment.text());
            chunks.add(Chunk.builder()
                    .text(segment.text())
                    .embedding(embeddingVector)
                    .clauseRef(clauseRefPrefix + " (part " + i + ")")
                    .build());
            i++;
        }
        return chunks;
    }

    public List<Chunk> ingestShared(String rawText, String clauseRefPrefix, String domain, String sourceId) {
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        Document document = Document.from(rawText);
        List<TextSegment> segments = splitter.split(document);

        ragChunkRepository.deleteBySourceId(sourceId);

        List<Chunk> chunks = new ArrayList<>();
        int i = 1;
        for (TextSegment segment : segments) {
            List<Double> embeddingVector = safeEmbed(segment.text());
            String clauseRef = clauseRefPrefix + " (part " + i + ")";

            chunks.add(Chunk.builder()
                    .text(segment.text())
                    .embedding(embeddingVector)
                    .clauseRef(clauseRef)
                    .build());

            ragChunkRepository.save(RagChunk.builder()
                    .domain(domain)
                    .sourceId(sourceId)
                    .clauseRef(clauseRef)
                    .text(segment.text())
                    .embedding(embeddingVector)
                    .build());
            i++;
        }
        return chunks;
    }

    // ===== ye naya method add karo =====
    public List<Chunk> embedProvidedChunks(List<com.bis.intelliguide.dto.request.StandardBulkImportRequest.ChunkInput> chunkInputs) {
        List<Chunk> chunks = new ArrayList<>();

        if (chunkInputs == null) {
            return chunks;
        }

        for (com.bis.intelliguide.dto.request.StandardBulkImportRequest.ChunkInput input : chunkInputs) {
            List<Double> embeddingVector = safeEmbed(input.getText());
            chunks.add(Chunk.builder()
                    .text(input.getText())
                    .embedding(embeddingVector)
                    .clauseRef(input.getClauseRef())
                    .build());
        }

        return chunks;
    }

    private List<Double> safeEmbed(String text) {
        try {
            return embeddingModel.embed(text).content().vectorAsList()
                    .stream()
                    .map(Float::doubleValue)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}