package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.request.CertificationRecommendRequest;
import com.bis.intelliguide.dto.response.CertificationAskResponse;
import com.bis.intelliguide.dto.response.CertificationRecommendResponse;
import com.bis.intelliguide.dto.response.CitationRef;
import com.bis.intelliguide.model.AiAnswerLog;
import com.bis.intelliguide.model.CertificationScheme;
import com.bis.intelliguide.repository.AiAnswerLogRepository;
import com.bis.intelliguide.repository.CertificationSchemeRepository;
import com.bis.intelliguide.service.rag.GenerationService;
import com.bis.intelliguide.service.translate.SarvamTranslateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CertificationGuideService {

    private final AiAnswerLogRepository aiAnswerLogRepository;
    private final CertificationSchemeRepository schemeRepository;
    private final GenerationService generationService;
    private final SarvamTranslateService translator;

    private static final Set<String> SIMPLIFIED_PROCEDURE_PRODUCTS = Set.of(
            "cement", "steel bars", "led lights", "packaged drinking water"
    );

    private static final Set<String> SCHEME_X_PRODUCTS = Set.of(
            "pressure cooker", "helmet", "toys"
    );

    public CertificationRecommendResponse recommend(CertificationRecommendRequest request, String userId) {
        String productType = request.getProductType() != null ? request.getProductType() : "";
        String productCategory = request.getProductCategory() != null ? request.getProductCategory() : "";
        String manufacturerType = request.getManufacturerType() != null ? request.getManufacturerType() : "";
        Boolean udyamRegistered = request.getUdyamRegistered();
        boolean managementSystemRequested =
                Boolean.TRUE.equals(request.getManagementSystemCertificationRequested());

        String schemeName;
        String matchReason;
        String confidence;

        if (productCategory.toLowerCase().contains("electronics")
                || productCategory.toLowerCase().contains("it")) {
            schemeName = "Scheme II — Compulsory Registration Scheme (CRS)";
            matchReason = "Product category falls under Electronics/IT, requiring CRS.";
            confidence = "HIGH";
        } else if ("FOREIGN".equalsIgnoreCase(manufacturerType)) {
            schemeName = "FMCS — Foreign Manufacturers Certification Scheme";
            matchReason = "Foreign manufacturer requires FMCS.";
            confidence = "HIGH";
        } else if ("MSME".equalsIgnoreCase(manufacturerType)
                && Boolean.TRUE.equals(udyamRegistered)
                && SIMPLIFIED_PROCEDURE_PRODUCTS.contains(productType.toLowerCase())) {
            schemeName = "Simplified Procedure (Option-2, under Scheme I)";
            matchReason = "MSME with Udyam registration, and product is on the Simplified Procedure list.";
            confidence = "HIGH";
        } else if (SCHEME_X_PRODUCTS.contains(productType.toLowerCase())) {
            schemeName = "Scheme-X Certification";
            matchReason = "Product is on the Scheme-X eligible product list.";
            confidence = "HIGH";
        } else if (managementSystemRequested) {
            schemeName = "System Certification (Management Systems Certification)";
            matchReason = "Request explicitly asked for a management-system certification.";
            confidence = "HIGH";
        } else {
            schemeName = "Scheme I (ISI Mark Scheme)";
            matchReason = "Default certification route for domestic manufacturers.";
            confidence = "MEDIUM";
        }

        Optional<CertificationScheme> schemeOpt = schemeRepository.findBySchemeName(schemeName)
                .filter(s -> "PUBLISHED".equals(s.getStatus()));

        aiAnswerLogRepository.save(AiAnswerLog.builder()
                .userId(userId)
                .question(productType + " | " + productCategory + " | " + manufacturerType)
                .agent("CERTIFICATION_GUIDE")
                .confidenceScore(schemeOpt.isPresent() ? 1.0 : 0.0)
                .sourceRef(schemeName)
                .flagged(schemeOpt.isEmpty())
                .reviewStatus("UNREVIEWED")
                .createdAt(Instant.now())
                .build());

        if (schemeOpt.isEmpty()) {
            return CertificationRecommendResponse.builder()
                    .recommendedScheme(schemeName)
                    .reason(translator.fromEnglish(matchReason))
                    .matchReason(translator.fromEnglish(matchReason))
                    .confidence(confidence)
                    .insufficientEvidence(true)
                    .references(List.of())
                    .build();
        }

        CertificationScheme scheme = schemeOpt.get();

        CertificationRecommendResponse.MatchedScheme matchedSchemeDto =
                CertificationRecommendResponse.MatchedScheme.builder()
                        .schemeName(scheme.getSchemeName())
                        .description(scheme.getDescription())
                        .eligibility(scheme.getEligibility())
                        .documentsRequired(scheme.getDocumentsRequired())
                        .processSteps(scheme.getProcessSteps() != null
                                ? scheme.getProcessSteps().stream()
                                .map(ps -> new CertificationRecommendResponse.StepDto(
                                        ps.getStep(),
                                        ps.getTitle() + ": " + ps.getDescription()))
                                .toList()
                                : List.of())
                        .estimatedTimeline(scheme.getEstimatedTimeline())
                        .version(scheme.getVersion())
                        .publishedAt(scheme.getPublishedAt())
                        .build();

        String structuredData = "Scheme: " + scheme.getSchemeName()
                + "\nDescription: " + scheme.getDescription()
                + "\nEligibility: " + scheme.getEligibility()
                + "\nDocuments required: "
                + (scheme.getDocumentsRequired() != null
                ? String.join(", ", scheme.getDocumentsRequired()) : "N/A")
                + "\nEstimated timeline: " + scheme.getEstimatedTimeline();

        String friendlyExplanation = generationService.generateFromKnownData(
                "Explain this certification scheme to a non-technical MSME owner in simple, encouraging, "
                        + "human language. Do not add any new fact not present in the data below. If a field "
                        + "is unclear or unspecified (e.g. timeline), say so honestly rather than guessing.",
                structuredData,
                null
        );

        List<CitationRef> references = scheme.getChunks() != null
                ? scheme.getChunks().stream()
                .map(c -> CitationRef.builder()
                        .doc(scheme.getSchemeName())
                        .clause(c.getClauseRef())
                        .build())
                .toList()
                : List.of();

        List<String> localizedDocs =
                translator.fromEnglishAll(scheme.getDocumentsRequired());

        List<CertificationRecommendResponse.StepDto> localizedSteps =
                localizeSteps(matchedSchemeDto.getProcessSteps());

        String localizedTimeline =
                translator.fromEnglish(scheme.getEstimatedTimeline());

        return CertificationRecommendResponse.builder()
                .recommendedScheme(schemeName)
                .reason(translator.fromEnglish(matchReason))
                .matchReason(translator.fromEnglish(matchReason))
                .confidence(confidence)
                .matchedScheme(matchedSchemeDto)
                .friendlyExplanation(friendlyExplanation)
                .processSteps(localizedSteps)
                .documentsRequired(localizedDocs)
                .estimatedTimeline(localizedTimeline)
                .references(references)
                .insufficientEvidence(false)
                .build();
    }

    private List<CertificationRecommendResponse.StepDto> localizeSteps(
            List<CertificationRecommendResponse.StepDto> steps) {

        if (steps == null || steps.isEmpty()) return steps;

        List<String> titles = translator.fromEnglishAll(
                steps.stream()
                        .map(CertificationRecommendResponse.StepDto::getTitle)
                        .toList());

        List<CertificationRecommendResponse.StepDto> out = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            out.add(new CertificationRecommendResponse.StepDto(
                    steps.get(i).getStep(),
                    titles.get(i)
            ));
        }

        return out;
    }

    public CertificationAskResponse askFollowUp(String schemeName, String query) {
        Optional<CertificationScheme> schemeOpt = schemeRepository.findBySchemeName(schemeName)
                .filter(s -> "PUBLISHED".equals(s.getStatus()));

        if (schemeOpt.isEmpty()) {
            return CertificationAskResponse.builder()
                    .answer(translator.fromEnglish(
                            "I don't have verified data for the scheme \""
                                    + schemeName
                                    + "\" to answer this confidently."))
                    .relatedScheme(schemeName)
                    .confidence("needs-verification")
                    .build();
        }

        CertificationScheme scheme = schemeOpt.get();

        String structuredData = "Scheme: " + scheme.getSchemeName()
                + "\nDescription: " + scheme.getDescription()
                + "\nEligibility: " + scheme.getEligibility()
                + "\nDocuments: "
                + (scheme.getDocumentsRequired() != null
                ? String.join(", ", scheme.getDocumentsRequired()) : "N/A")
                + "\nTimeline: " + scheme.getEstimatedTimeline();

        String answer = generationService.generateFromKnownData(
                "Answer the user's follow-up question about this certification scheme, using ONLY the data "
                        + "below. If the question is out of scope for this scheme (e.g. about hallmarking or a "
                        + "different topic entirely), say so plainly rather than attempting a generic reply.",
                structuredData,
                query
        );

        return CertificationAskResponse.builder()
                .answer(answer)
                .relatedScheme(schemeName)
                .confidence("high")
                .build();
    }
}