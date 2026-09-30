package com.bis.intelliguide.service.agents;


import com.bis.intelliguide.dto.request.HallmarkAskRequest;
import com.bis.intelliguide.dto.response.AssistantResponse;
import com.bis.intelliguide.dto.response.ComplaintGuidanceResponse;
import com.bis.intelliguide.dto.response.HuidVerifyResponse;
import com.bis.intelliguide.dto.response.NearestCentreResponse;
import com.bis.intelliguide.dto.response.PurityInfoResponse;
import com.bis.intelliguide.model.HuidRecord;
import com.bis.intelliguide.repository.HuidRecordRepository;
import com.bis.intelliguide.service.rag.GenerationResult;
import com.bis.intelliguide.service.rag.GenerationService;
import com.bis.intelliguide.service.translate.SarvamTranslateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.bis.intelliguide.model.AhcCentre;
import com.bis.intelliguide.repository.AhcCentreRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HallmarkingService {

    private final HuidRecordRepository huidRecordRepository;
    private final GenerationService generationService;
    private final AhcCentreRepository ahcCentreRepository;
    private final SarvamTranslateService translator;

    public HuidVerifyResponse verifyHuid(String huid) {
        Optional<HuidRecord> record = huidRecordRepository.findByHuid(huid);

        if (record.isEmpty()) {
            return HuidVerifyResponse.builder()
                    .verified(false)
                    .message("No record found for this HUID. Double-check the 6-digit code, "
                            + "or this may not be a genuine BIS-hallmarked article.")
                    .build();
        }

        HuidRecord r = record.get();
        return HuidVerifyResponse.builder()
                .verified(true)
                .purity(r.getPurity())
                .ahcCentre(r.getAhcCentre())
                .hallmarkedOn(r.getHallmarkedOn())
                .message("Verified against BIS hallmarking records.")
                .build();
    }

    public NearestCentreResponse findNearestCentres(double lat, double lng) {
        List<AhcCentre> allCentres = ahcCentreRepository.findAll();

        List<NearestCentreResponse.CentreDto> sorted = allCentres.stream()
                .filter(c -> c.getLocation() != null && c.getLocation().getCoordinates() != null)
                .map(c -> {
                    double centreLng = c.getLocation().getCoordinates()[0];
                    double centreLat = c.getLocation().getCoordinates()[1];
                    double dist = haversineKm(lat, lng, centreLat, centreLng);

                    return NearestCentreResponse.CentreDto.builder()
                            .name(c.getName())
                            .city(c.getCity())
                            .state(c.getState())
                            .address(c.getAddress())
                            .distanceKm(Math.round(dist * 10.0) / 10.0)
                            .build();
                })
                .sorted(Comparator.comparingDouble(
                        NearestCentreResponse.CentreDto::getDistanceKm
                ))
                .toList();

        return NearestCentreResponse.builder()
                .centres(sorted.subList(0, Math.min(3, sorted.size())))
                .build();
    }

    public Map<String, Object> getJewellerRegistrationInfo() {
        return Map.of("steps", translator.fromEnglishAll(List.of(
                "Apply on manakonline.in",
                "Certificate granted instantly, no documents or fee required"
        )));
    }

    public PurityInfoResponse getPurityInfo(String code) {
        Map<String, PurityInfoResponse> purityMap = Map.of(
                "24K999", PurityInfoResponse.builder()
                        .karat("24K")
                        .purityPercent(99.9)
                        .description("24 karat gold, 99.9% pure")
                        .build(),

                "22K916", PurityInfoResponse.builder()
                        .karat("22K")
                        .purityPercent(91.6)
                        .description("22 karat gold, 91.6% pure")
                        .build(),

                "18K750", PurityInfoResponse.builder()
                        .karat("18K")
                        .purityPercent(75.0)
                        .description("18 karat gold, 75% pure")
                        .build(),

                "14K585", PurityInfoResponse.builder()
                        .karat("14K")
                        .purityPercent(58.5)
                        .description("14 karat gold, 58.5% pure")
                        .build()
        );

        PurityInfoResponse result = purityMap.get(
                code.replaceAll("[^A-Za-z0-9]", "").toUpperCase()
        );

        if (result == null) {
            return PurityInfoResponse.builder()
                    .karat("Unknown")
                    .purityPercent(0)
                    .description(translator.fromEnglish(
                            "Purity code '" + code
                                    + "' not recognized. Valid codes: 24K999, 22K916, 18K750, 14K585"
                    ))
                    .build();
        }

        return PurityInfoResponse.builder()
                .karat(result.getKarat())
                .purityPercent(result.getPurityPercent())
                .description(translator.fromEnglish(result.getDescription()))
                .build();
    }

    public ComplaintGuidanceResponse getComplaintGuidance(String query) {
        GenerationResult result = generationService.generateGroundedAnswer(
                query,
                "consumer_protection"
        );

        return ComplaintGuidanceResponse.builder()
                .answer(result.getAnswer())
                .applicableClause(
                        result.getCitations() != null && !result.getCitations().isEmpty()
                                ? result.getCitations().get(0).getClause()
                                : "BIS Rules 2018"
                )
                .compensationInfo(
                        "As per BIS Hallmarking Regulations, consumers may be entitled to 2x the value of the purity shortfall plus testing charges."
                )
                .build();
    }

    public AssistantResponse askFollowUp(HallmarkAskRequest request) {
        String enrichedQuery = request.getQuery();

        if (request.getContext() != null) {
            String lastHuid = request.getContext().get("lastHuidChecked");
            String lastResult = request.getContext().get("lastResult");

            if (lastHuid != null) {
                enrichedQuery += " (Previously checked HUID: " + lastHuid + ")";
            }

            if (lastResult != null) {
                enrichedQuery += " (Previous result: " + lastResult + ")";
            }
        }

        GenerationResult result = generationService.generateGroundedAnswer(
                enrichedQuery,
                "bis_services"
        );

        return AssistantResponse.builder()
                .answer(result.getAnswer())
                .shouldRedirect(false)
                .build();
    }

    private double haversineKm(
            double lat1,
            double lng1,
            double lat2,
            double lng2
    ) {
        double R = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2)
                        * Math.sin(dLng / 2);

        return R * 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );
    }
}


