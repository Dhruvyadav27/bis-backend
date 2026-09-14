package com.bis.intelliguide.service.orchestrator;

import com.bis.intelliguide.dto.request.CertificationRecommendRequest;
import com.bis.intelliguide.dto.request.JourneyStartRequest;
import com.bis.intelliguide.dto.response.*;
import com.bis.intelliguide.model.UserJourneyProgress;
import com.bis.intelliguide.repository.UserJourneyProgressRepository;
import com.bis.intelliguide.service.agents.CertificationGuideService;
import com.bis.intelliguide.service.agents.FindLabService;
import com.bis.intelliguide.service.agents.StandardFinderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * THE FLAGSHIP FEATURE. Never answers on its own — it determines the user's overall
 * goal, breaks it into an ordered sequence of steps, and at each step calls the
 * relevant worker agent internally, explaining the result back in simple language
 * and proactively telling the user what happens next. Progress is saved so the
 * user can leave and resume later (user_journey_progress).
 *
 * MVP demo path (per project brief §10): Standard Finder -> Certification Guide -> Find Lab.
 */
@Service
@RequiredArgsConstructor
public class ProcessGuideOrchestratorService {

    private final UserJourneyProgressRepository journeyRepository;
    private final StandardFinderService standardFinderService;
    private final CertificationGuideService certificationGuideService;
    private final FindLabService findLabService;

    private static final String JOURNEY_TYPE = "GET_PRODUCT_CERTIFIED";

    public JourneyResponse start(JourneyStartRequest request, String userId) {
        List<UserJourneyProgress.Stage> stages = new ArrayList<>();
        stages.add(stage(1, "Identify applicable standard", "PENDING", null, null));
        stages.add(stage(2, "Explain certification scheme & requirements", "PENDING", null, null));
        stages.add(stage(3, "Find a suitable testing lab", "PENDING", null, null));
        stages.add(stage(4, "Final summary & next recommended action", "PENDING", null, null));

        UserJourneyProgress journey = UserJourneyProgress.builder()
                .userId(userId)
                .journeyType(JOURNEY_TYPE)
                .currentStep(1)
                .stages(stages)
                .updatedAt(Instant.now())
                .build();

        journey = journeyRepository.save(journey);

        // Immediately execute step 1 so the user sees proactive progress right away,
        // instead of just an empty checklist.
        journey = runStep1_StandardFinder(journey, request.getProductDescription());

        return toResponse(journey);
    }

    public JourneyResponse getCurrent(String userId) {
        Optional<UserJourneyProgress> journey = journeyRepository.findTopByUserIdOrderByUpdatedAtDesc(userId);
        return journey.map(this::toResponse).orElse(null);
    }

    public JourneyAdvanceResponse advance(String journeyId, String action, String userId,
                                           String manufacturerType, String state, String district) {
        UserJourneyProgress journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));

        int step = journey.getCurrentStep();
        String message;

        switch (step) {
            case 1 -> {
                // Step 1 already ran at start(); advance moves us to step 2.
                journey.setCurrentStep(2);
                journey = runStep2_CertificationGuide(journey, manufacturerType);
                message = "Certification requirements identified. Next: finding a nearby testing lab.";
            }
            case 2 -> {
                journey.setCurrentStep(3);
                journey = runStep3_FindLab(journey, state, district);
                message = "Nearby labs found. Next: final summary of your certification journey.";
            }
            case 3 -> {
                journey.setCurrentStep(4);
                markStageDone(journey, 4, "All steps completed. You now have: the applicable standard, "
                        + "the certification scheme & documents needed, and a nearby recognized lab. "
                        + "Next recommended action: book a slot at the lab and begin sample testing.");
                message = "Journey complete — see the final summary.";
            }
            default -> message = "Journey already complete.";
        }

        journey.setUpdatedAt(Instant.now());
        journeyRepository.save(journey);

        return JourneyAdvanceResponse.builder()
                .currentStep(journey.getCurrentStep())
                .message(message)
                .build();
    }

    private UserJourneyProgress runStep1_StandardFinder(UserJourneyProgress journey, String productDescription) {
        StandardSearchResponse result = standardFinderService.search(productDescription, journey.getUserId());
        markStageDone(journey, 1, result);
        return journeyRepository.save(journey);
    }

    private UserJourneyProgress runStep2_CertificationGuide(UserJourneyProgress journey, String manufacturerType) {
        CertificationRecommendRequest req = new CertificationRecommendRequest();
        req.setProductType("product from journey"); // in a full build, carry the original product description forward
        req.setManufacturerType(manufacturerType);
        CertificationRecommendResponse result = certificationGuideService.recommend(req, journey.getUserId());
        markStageDone(journey, 2, result);
        return journeyRepository.save(journey);
    }

    private UserJourneyProgress runStep3_FindLab(UserJourneyProgress journey, String state, String district) {
        LabSearchResponse result = findLabService.search(null, state, district);
        markStageDone(journey, 3, result);
        return journeyRepository.save(journey);
    }

    private void markStageDone(UserJourneyProgress journey, int step, Object result) {
        journey.getStages().stream()
                .filter(s -> s.getStep() == step)
                .findFirst()
                .ifPresent(s -> {
                    s.setStatus("DONE");
                    s.setResult(result);
                });
    }

    private UserJourneyProgress.Stage stage(int step, String title, String status, Object result, String agentUsed) {
        return UserJourneyProgress.Stage.builder()
                .step(step).title(title).status(status).result(result).agentUsed(agentUsed).build();
    }

    private JourneyResponse toResponse(UserJourneyProgress journey) {
        List<JourneyResponse.StageDto> stageDtos = journey.getStages().stream()
                .map(s -> JourneyResponse.StageDto.builder()
                        .step(s.getStep()).title(s.getTitle()).status(s.getStatus()).result(s.getResult())
                        .build())
                .toList();

        return JourneyResponse.builder()
                .journeyId(journey.getId())
                .currentStep(journey.getCurrentStep())
                .stages(stageDtos)
                .build();
    }
}
