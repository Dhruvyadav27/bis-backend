package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.RagChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RagChunkRepository extends MongoRepository<RagChunk, String> {
    void deleteBySourceId(String sourceId);
}
