package com.bis.intelliguide.service.orchestrator;

import com.bis.intelliguide.dto.request.CertificationRecommendRequest;
import com.bis.intelliguide.dto.request.JourneyStartRequest;
import com.bis.intelliguide.dto.response.*;
import com.bis.intelliguide.model.UserJourneyProgress;
import com.bis.intelliguide.repository.UserJourneyProgressRepository;
import com.bis.intelliguide.service.agents.CertificationGuideService;
import com.bis.intelliguide.service.agents.FindLabService;
import com.bis.intelliguide.service.agents.StandardFinderService;
import com.bis.intelliguide.service.rag.GenerationService;
import com.bis.intelliguide.service.rag.GenerationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProcessGuideOrchestratorService {

    private final UserJourneyProgressRepository journeyRepository;
    private final StandardFinderService standardFinderService;
    private final CertificationGuideService certificationGuideService;
    private final FindLabService findLabService;
    private final GenerationService generationService;

    private static final String JOURNEY_TYPE = "GET_PRODUCT_CERTIFIED";

    public JourneyResponse start(JourneyStartRequest request, String userId) {
        List<UserJourneyProgress.Stage> stages = new ArrayList<>();
        stages.add(stage(1, "Identify applicable standard", "PENDING", null, "StandardFinder"));
        stages.add(stage(2, "Check certification scheme", "PENDING", null, "CertificationGuide"));
        stages.add(stage(3, "Find testing lab", "PENDING", null, "FindLab"));
        stages.add(stage(4, "List required documents", "PENDING", null, "CertificationGuide"));
        stages.add(stage(5, "Journey summary", "PENDING", null, null));

        UserJourneyProgress journey = UserJourneyProgress.builder()
                .userId(userId)
                .journeyType(JOURNEY_TYPE)
                .currentStep(1)
                .stages(stages)
                .updatedAt(Instant.now())
                .productTitle(request.getProductTitle())
                .productDescription(request.getProductDescription())
                .state(request.getState())
                .district(request.getDistrict())
                .manufacturerType(request.getManufacturerType())
                .status("IN_PROGRESS")
                .createdAt(Instant.now())
                .build();

        journey = journeyRepository.save(journey);

        journey = runStep1_StandardFinder(journey, request.getProductDescription());

        return toResponse(journey);
    }

    public JourneyResponse getCurrent(String userId) {
        Optional<UserJourneyProgress> journey = journeyRepository.findTopByUserIdOrderByUpdatedAtDesc(userId);
        return journey.map(this::toResponse).orElse(null);
    }

    public JourneyResponse getJourney(String journeyId) {
        UserJourneyProgress journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));
        return toResponse(journey);
    }

    public JourneyAdvanceResponse advance(String journeyId, int step, String action, String userId, Double lat, Double lng) {
        UserJourneyProgress journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));

        if (step != journey.getCurrentStep()) {
            throw new IllegalArgumentException("Step does not match current step");
        }

        if ("SKIP".equalsIgnoreCase(action)) {
            markStageStatus(journey, step, "SKIPPED", journey.getStages().get(step - 1).getResult());
        }

        String message = "Moved to next step";
        journey.setCurrentStep(step + 1);

        switch (step) {
            case 1 -> journey = runStep2_CertificationGuide(journey, journey.getManufacturerType());
            case 2 -> journey = runStep3_FindLab(journey, journey.getState(), journey.getDistrict(), lat, lng);
            case 3 -> journey = runStep4_ListDocuments(journey, journey.getManufacturerType());
            case 4 -> {
                markStageStatus(journey, 5, "DONE", "All steps completed. Summary: You have identified the standard, certification scheme, lab, and documents.");
                journey.setStatus("COMPLETED");
                message = "Journey complete — see the final summary.";
            }
        }

        journey.setUpdatedAt(Instant.now());
        journeyRepository.save(journey);

        return JourneyAdvanceResponse.builder()
                .currentStep(journey.getCurrentStep())
                .message(message)
                .stages(toResponse(journey).getStages())
                .build();
    }

    public JourneyAskResponse ask(String journeyId, String query) {
        UserJourneyProgress journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));

        int currentStep = journey.getCurrentStep();
        String agentUsed = journey.getStages().stream()
                .filter(s -> s.getStep() == currentStep)
                .findFirst()
                .map(UserJourneyProgress.Stage::getAgentUsed)
                .orElse(null);

        String collection = switch (agentUsed == null ? "" : agentUsed) {
            case "StandardFinder" -> "standards";
            case "CertificationGuide" -> "certification_schemes";
            case "FindLab" -> "bis_services";
            default -> "standards";
        };

        GenerationResult result = generationService.generateGroundedAnswer(
                "Product: " + journey.getProductDescription() + "\nQuestion: " + query,
                collection);

        return JourneyAskResponse.builder()
                .answer(result.getAnswer())
                .relatedStep(currentStep)
                .build();
    }

    private UserJourneyProgress runStep1_StandardFinder(UserJourneyProgress journey, String productDescription) {
        StandardSearchResponse result = standardFinderService.search(productDescription, journey.getUserId());
        markStageStatus(journey, 1, "DONE", result);
        return journeyRepository.save(journey);
    }

    private UserJourneyProgress runStep2_CertificationGuide(UserJourneyProgress journey, String manufacturerType) {
        CertificationRecommendRequest req = new CertificationRecommendRequest();
        req.setProductType("product from journey");
        req.setManufacturerType(manufacturerType);
        CertificationRecommendResponse result = certificationGuideService.recommend(req, journey.getUserId());
        markStageStatus(journey, 2, "DONE", result);
        return journeyRepository.save(journey);
    }

    private UserJourneyProgress runStep3_FindLab(UserJourneyProgress journey, String state, String district, Double lat, Double lng) {
        LabSearchResponse result;
        if (lat != null && lng != null) {
            result = findLabService.findNearest(journey.getProductDescription(), lat, lng);
        } else {
            result = findLabService.search(null, state, district);
        }
        markStageStatus(journey, 3, "DONE", result);
        return journeyRepository.save(journey);
    }

    private UserJourneyProgress runStep4_ListDocuments(UserJourneyProgress journey, String manufacturerType) {
        CertificationRecommendRequest req = new CertificationRecommendRequest();
        req.setProductType("product from journey");
        req.setManufacturerType(manufacturerType);
        CertificationRecommendResponse result = certificationGuideService.recommend(req, journey.getUserId());
        markStageStatus(journey, 4, "DONE", result);
        return journeyRepository.save(journey);
    }

    private void markStageStatus(UserJourneyProgress journey, int step, String status, Object result) {
        journey.getStages().stream()
                .filter(s -> s.getStep() == step)
                .findFirst()
                .ifPresent(s -> {
                    s.setStatus(status);
                    if (result != null) {
                        s.setResult(result);
                    }
                });
    }

    private UserJourneyProgress.Stage stage(int step, String title, String status, Object result, String agentUsed) {
        return UserJourneyProgress.Stage.builder()
                .step(step).title(title).status(status).result(result).agentUsed(agentUsed).build();
    }

    private JourneyResponse toResponse(UserJourneyProgress journey) {
        List<JourneyResponse.StageDto> stageDtos = journey.getStages().stream()
                .map(s -> JourneyResponse.StageDto.builder()
                        .step(s.getStep())
                        .title(s.getTitle())
                        .status(s.getStatus())
                        .result(s.getResult())
                        .agentUsed(s.getAgentUsed())
                        .build())
                .toList();

        return JourneyResponse.builder()
                .journeyId(journey.getId())
                .currentStep(journey.getCurrentStep())
                .status(journey.getStatus())
                .productTitle(journey.getProductTitle())
                .productDescription(journey.getProductDescription())
                .stages(stageDtos)
                .build();
    }
}
