package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.request.ComplaintRequest;
import com.bis.intelliguide.dto.response.ComplaintResponse;
import com.bis.intelliguide.dto.response.ConsumerAskResponse;
import com.bis.intelliguide.model.Complaint;
import com.bis.intelliguide.repository.ComplaintRepository;
import com.bis.intelliguide.service.rag.GenerationResult;
import com.bis.intelliguide.service.rag.GenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Two modes, per the updated brief:
 *   1. General query mode (ask()) — free-form consumer questions, same RAG pattern
 *      as the other worker agents. NOT limited to complaints.
 *   2. Complaint mode (fileComplaint()) — structured complaint filing, one use case
 *      within this agent, not its entire scope.
 */
@Service
@RequiredArgsConstructor
public class ConsumerAffairsService {

    private final GenerationService generationService;
    private final ComplaintRepository complaintRepository;

    public ConsumerAskResponse ask(String query) {
        GenerationResult result = generationService.generateGroundedAnswer(query, "consumer_protection");

        return ConsumerAskResponse.builder()
                .answer(result.getAnswer())
                .references(result.getCitations())
                .confidence(result.getConfidenceLabel())
                .insufficientEvidence(result.isInsufficientEvidence())
                .build();
    }

    public ComplaintResponse fileComplaint(ComplaintRequest request, String userId) {
        GenerationResult result = generationService.generateGroundedAnswer(
                request.getComplaintType() + ": " + request.getProductDetails(), "consumer_protection");

        String referenceId = "BIS-CA-" + java.time.Year.now().getValue() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String status = result.isInsufficientEvidence() ? "UNDER_REVIEW" : "FILED";

        Complaint complaint = Complaint.builder()
                .referenceId(referenceId)
                .userId(userId)
                .complaintType(request.getComplaintType())
                .productDetails(request.getProductDetails())
                .licenseOrHuidNumber(request.getLicenseOrHuidNumber())
                .evidenceUrl(request.getEvidenceUrl())
                .applicableClause(result.getCitations().isEmpty() ? null : result.getCitations().get(0).getClause())
                .confidenceScore(result.getConfidenceScore())
                .status(status)
                .redirectedToBis(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        complaintRepository.save(complaint);

        return ComplaintResponse.builder()
                .complaintId(referenceId)
                .steps(List.of(
                        "Your complaint has been logged with reference " + referenceId,
                        "Our team will verify the applicable clause and evidence",
                        "For an official BIS record, also file via manakonline.in or the BIS CARE app"
                ))
                .applicableClause(complaint.getApplicableClause())
                .status(status)
                .redirectUrl("https://www.manakonline.in")
                .build();
    }
}
