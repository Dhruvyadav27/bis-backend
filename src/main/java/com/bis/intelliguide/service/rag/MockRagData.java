package com.bis.intelliguide.service.rag;

import java.util.List;

/**
 * Seeded mock retrieval results, used until the real MongoDB Atlas Vector Search
 * indexes are populated via the admin ingestion pipeline. Every controller in this
 * project already calls through RetrievalService, so swapping this out later requires
 * no controller/service-layer changes — only RetrievalService.retrieve() itself.
 */
public class MockRagData {

    public static List<RetrievedChunk> mockChunksFor(String collection, String query) {
        String q = query == null ? "" : query.toLowerCase();

        if (collection.equals("standards")) {
            if (q.contains("helmet")) {
                return List.of(
                    RetrievedChunk.builder().text("Protective helmets for two-wheeler riders — specification.")
                        .clauseRef("IS 4151:2015 Clause 5.2").sourceId("std-4151").score(0.94).build(),
                    RetrievedChunk.builder().text("Related: shell material impact resistance requirements.")
                        .clauseRef("IS 4151:2015 Clause 6.1").sourceId("std-4151").score(0.88).build()
                );
            }
            if (q.contains("led") || q.contains("light")) {
                return List.of(
                    RetrievedChunk.builder().text("Self-ballasted LED lamps — safety requirements.")
                        .clauseRef("IS 16102 (Part 1):2012").sourceId("std-16102").score(0.91).build()
                );
            }
            // generic fallback match with a lower score to demonstrate the "moderate" band
            return List.of(
                RetrievedChunk.builder().text("General product safety and quality specification clause.")
                    .clauseRef("IS 302:2008 Clause 3.1").sourceId("std-302").score(0.76).build()
            );
        }

        if (collection.equals("certification_schemes")) {
            return List.of(
                RetrievedChunk.builder().text("Scheme-I: Normal Procedure — ISI Mark Licensing for domestic manufacturers.")
                    .clauseRef("BIS Certification Scheme-I").sourceId("scheme-1").score(0.89).build()
            );
        }

        if (collection.equals("bis_services")) {
            return List.of(
                RetrievedChunk.builder().text("Hallmarking registration procedure for jewellers under BIS.")
                    .clauseRef("Hallmarking Scheme Clause 4").sourceId("svc-hm-1").score(0.85).build()
            );
        }

        if (collection.equals("consumer_protection")) {
            return List.of(
                RetrievedChunk.builder().text("Consumers may report misuse of the ISI/Hallmark and seek redress.")
                    .clauseRef("Consumer Protection Act 2019, Sec 2(47)").sourceId("cpa-2019").score(0.82).build()
            );
        }

        // Unknown collection or genuinely nothing relevant — force the "insufficient evidence" path.
        return List.of();
    }
}
