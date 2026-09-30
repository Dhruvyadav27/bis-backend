package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.EntityVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EntityVersionRepository extends MongoRepository<EntityVersion, String> {
    List<EntityVersion> findByEntityTypeAndEntityIdOrderByVersionNumberDesc(String entityType, String entityId);
    Optional<EntityVersion> findByEntityTypeAndEntityIdAndVersionNumber(String entityType, String entityId, int versionNumber);
}