package com.bis.intelliguide.service.admin;

import com.bis.intelliguide.model.EntityVersion;
import com.bis.intelliguide.repository.EntityVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VersionHistoryService {

    private final EntityVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    /** Fields never stored in a snapshot — chunks/embeddings are derived on publish, not user-edited content. */
    private static final Set<String> EXCLUDED_FIELDS = Set.of("chunks", "id");

    @SuppressWarnings("unchecked")
    public void snapshot(String entityType, String entityId, int versionNumber, Object entity,
                         String editedBy, String action, String reason) {
        Map<String, Object> raw = objectMapper.convertValue(entity, Map.class);
        Map<String, Object> clean = new HashMap<>(raw);
        EXCLUDED_FIELDS.forEach(clean::remove);

        versionRepository.save(EntityVersion.builder()
                .entityType(entityType)
                .entityId(entityId)
                .versionNumber(versionNumber)
                .action(action)
                .reason(reason)
                .editedBy(editedBy)
                .timestamp(Instant.now())
                .snapshot(clean)
                .build());
    }

    public List<EntityVersion> history(String entityType, String entityId) {
        return versionRepository.findByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId);
    }

    public EntityVersion getVersion(String entityType, String entityId, int versionNumber) {
        return versionRepository.findByEntityTypeAndEntityIdAndVersionNumber(entityType, entityId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No version " + versionNumber + " found for " + entityType + " " + entityId));
    }

    /**
     * Merges a past snapshot's field values back onto the current entity object.
     * `id`, `status`, `version`, `publishedAt`, `createdBy`, `chunks` are never restored —
     * restoring content should not silently republish or change ownership metadata.
     */
    @SuppressWarnings("unchecked")
    public <T> T applySnapshot(T current, Map<String, Object> snapshot, Class<T> clazz) {
        Set<String> neverRestore = Set.of("id", "status", "version", "publishedAt", "createdBy", "chunks", "lastEditedBy");
        Map<String, Object> currentMap = new HashMap<>(objectMapper.convertValue(current, Map.class));
        snapshot.forEach((k, v) -> {
            if (!neverRestore.contains(k)) {
                currentMap.put(k, v);
            }
        });
        return objectMapper.convertValue(currentMap, clazz);
    }
}