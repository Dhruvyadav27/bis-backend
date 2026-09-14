package com.bis.intelliguide.service.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetrievalService {

    private final EmbeddingModel embeddingModel;
    private final MongoTemplate mongoTemplate;

    @Value("${app.rag.top-k}")
    private int topK;

    public List<RetrievedChunk> retrieve(String query, String collection) {
        if (!isVectorSearchable(collection)) {
            return MockRagData.mockChunksFor(collection, query);
        }

        List<Double> queryEmbedding = safeEmbed(query);
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            log.warn("Embedding call failed or returned empty — falling back to mock data for collection '{}'", collection);
            return MockRagData.mockChunksFor(collection, query);
        }

        try {
            return runVectorSearch(collection, queryEmbedding);
        } catch (Exception e) {
            log.warn("Atlas $vectorSearch failed for collection '{}' (index missing or Atlas unreachable): {}. "
                    + "Falling back to mock data.", collection, e.getMessage());
            return MockRagData.mockChunksFor(collection, query);
        }
    }

    private boolean isVectorSearchable(String collection) {
        return collection.equals("standards")
                || collection.equals("certification_schemes")
                || collection.equals("bis_services")
                || collection.equals("consumer_protection");
    }

    private List<RetrievedChunk> runVectorSearch(String collection, List<Double> queryEmbedding) {
        if (collection.equals("standards")) {
            return runVectorSearchOnStandards(queryEmbedding);
        }
        return runVectorSearchOnSharedCollection(collection, queryEmbedding);
    }

    @SuppressWarnings("unchecked")
    private List<RetrievedChunk> runVectorSearchOnStandards(List<Double> queryEmbedding) {
        Document vectorSearchStage = new Document("$vectorSearch", new Document()
                .append("index", "vector_index")
                .append("path", "chunks.embedding")
                .append("queryVector", queryEmbedding)
                .append("numCandidates", Math.max(100, topK * 20))
                .append("limit", topK)
        );

        Document unwindStage = new Document("$unwind", "$chunks");

        Document projectStage = new Document("$project", new Document()
                .append("text", "$chunks.text")
                .append("clauseRef", "$chunks.clauseRef")
                .append("sourceId", new Document("$toString", "$_id"))
                .append("score", new Document("$meta", "vectorSearchScore"))
        );

        Document limitStage = new Document("$limit", topK);

        List<Document> pipeline = List.of(vectorSearchStage, unwindStage, projectStage, limitStage);

        List<Document> rawResults = mongoTemplate.getCollection("standards")
                .aggregate(pipeline)
                .into(new ArrayList<>());

        return toRetrievedChunks(rawResults);
    }

    /** Used for certification_schemes / bis_services / consumer_protection — all share
     *  the "rag_content" collection, filtered by the `domain` field. */
    private List<RetrievedChunk> runVectorSearchOnSharedCollection(String domain, List<Double> queryEmbedding) {
        Document vectorSearchStage = new Document("$vectorSearch", new Document()
                .append("index", "vector_index")
                .append("path", "embedding")
                .append("queryVector", queryEmbedding)
                .append("filter", new Document("domain", domain))
                .append("numCandidates", Math.max(100, topK * 20))
                .append("limit", topK)
        );

        Document projectStage = new Document("$project", new Document()
                .append("text", "$text")
                .append("clauseRef", "$clauseRef")
                .append("sourceId", "$sourceId")
                .append("score", new Document("$meta", "vectorSearchScore"))
        );

        List<Document> pipeline = List.of(vectorSearchStage, projectStage);

        List<Document> rawResults = mongoTemplate.getCollection("rag_content")
                .aggregate(pipeline)
                .into(new ArrayList<>());

        return toRetrievedChunks(rawResults);
    }

    private List<RetrievedChunk> toRetrievedChunks(List<Document> rawResults) {
        List<RetrievedChunk> chunks = new ArrayList<>();
        for (Document doc : rawResults) {
            chunks.add(RetrievedChunk.builder()
                    .text(doc.getString("text"))
                    .clauseRef(doc.getString("clauseRef"))
                    .sourceId(doc.getString("sourceId"))
                    .score(doc.get("score", Number.class) != null ? doc.get("score", Number.class).doubleValue() : 0.0)
                    .build());
        }
        return chunks;
    }

    private List<Double> safeEmbed(String query) {
        try {
            Embedding embedding = embeddingModel.embed(query).content();
            List<Float> floatVector = embedding.vectorAsList();
            List<Double> doubleVector = new ArrayList<>();
            for (Float f : floatVector) {
                doubleVector.add(f.doubleValue());
            }
            return doubleVector;
        } catch (Exception e) {
            log.warn("Embedding API call failed: {}", e.getMessage());
            return null;
        }
    }
}