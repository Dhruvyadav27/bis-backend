package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.response.LabSearchResponse;
import com.bis.intelliguide.model.Lab;
import com.bis.intelliguide.model.LabGeolocation;
import com.bis.intelliguide.repository.LabGeolocationRepository;
import com.bis.intelliguide.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindLabService {

    private final LabRepository labRepository;
    private final LabGeolocationRepository labGeolocationRepository;

    /**
     * GPS-based nearest-lab search per spec.
     * 1. Extract scope keyword from productDescription.
     * 2. Filter labs by scope (case-insensitive regex).
     * 3. Batch-lookup geolocation for candidates.
     * 4. Compute Haversine distance, sort ascending, take top 3.
     */
    public LabSearchResponse findNearest(String productDescription, double lat, double lng) {
        // Step 1: Get all recognized labs
        List<Lab> allLabs = labRepository.findAll().stream()
                .filter(l -> "RECOGNIZED".equalsIgnoreCase(l.getRecognitionStatus()))
                .toList();

        // Step 2: Filter by scope keyword from product description
        List<Lab> candidates;
        if (StringUtils.hasText(productDescription)) {
            String keyword = extractScopeKeyword(productDescription);
            candidates = allLabs.stream()
                    .filter(l -> l.getScope() != null
                            && l.getScope().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
            // If no scope match, fall back to all recognized labs
            if (candidates.isEmpty()) {
                candidates = allLabs;
            }
        } else {
            candidates = allLabs;
        }

        // Step 3: Batch-lookup geolocation
        List<String> labNames = candidates.stream().map(Lab::getName).toList();
        Map<String, LabGeolocation> geoMap = labGeolocationRepository.findByLabNameIn(labNames)
                .stream()
                .collect(Collectors.toMap(LabGeolocation::getLabName, g -> g, (a, b) -> a));

        // Step 4: Compute distance, sort, take top 3
        List<LabSearchResponse.LabDto> results = new ArrayList<>();
        for (Lab lab : candidates) {
            LabGeolocation geo = geoMap.get(lab.getName());
            if (geo == null || geo.getLocation() == null || geo.getLocation().getCoordinates() == null) {
                continue; // Skip labs without geolocation
            }

            double labLng = geo.getLocation().getCoordinates()[0];
            double labLat = geo.getLocation().getCoordinates()[1];
            double distanceKm = haversineKm(lat, lng, labLat, labLng);

            results.add(LabSearchResponse.LabDto.builder()
                    .name(lab.getName())
                    .city(geo.getCity())
                    .state(lab.getState())
                    .district(lab.getDistrict())
                    .address(geo.getAddress() != null ? geo.getAddress() : lab.getAddress())
                    .distanceKm(Math.round(distanceKm * 10.0) / 10.0)
                    .scope(lab.getScope())
                    .workingHours(lab.getWorkingHours())
                    .recognitionStatus(lab.getRecognitionStatus())
                    .latitude(labLat)
                    .longitude(labLng)
                    .build());
        }

        results.sort(Comparator.comparingDouble(LabSearchResponse.LabDto::getDistanceKm));
        List<LabSearchResponse.LabDto> top3 = results.subList(0, Math.min(3, results.size()));

        return LabSearchResponse.builder().labs(top3).build();
    }

    /**
     * Legacy search method (kept for backward compatibility with orchestrator).
     */
    public LabSearchResponse search(String query, String state, String district) {
        List<Lab> labs;
        if (StringUtils.hasText(state) && StringUtils.hasText(district)) {
            labs = labRepository.findByStateIgnoreCaseAndDistrictIgnoreCase(state, district);
        } else if (StringUtils.hasText(state)) {
            labs = labRepository.findByStateIgnoreCase(state);
        } else {
            labs = labRepository.findAll();
        }

        List<LabSearchResponse.LabDto> results = labs.stream()
                .filter(l -> "RECOGNIZED".equalsIgnoreCase(l.getRecognitionStatus()))
                .filter(l -> !StringUtils.hasText(query)
                        || (l.getScope() != null && l.getScope().toLowerCase().contains(query.toLowerCase())))
                .sorted(Comparator.comparingDouble(Lab::getDistanceMeta))
                .map(l -> LabSearchResponse.LabDto.builder()
                        .name(l.getName())
                        .state(l.getState())
                        .district(l.getDistrict())
                        .distanceKm(l.getDistanceMeta())
                        .scope(l.getScope())
                        .workingHours(l.getWorkingHours())
                        .recognitionStatus(l.getRecognitionStatus())
                        .build())
                .toList();

        return LabSearchResponse.builder().labs(results).build();
    }

    private String extractScopeKeyword(String productDescription) {
        String desc = productDescription.toLowerCase();
        if (desc.contains("electric") || desc.contains("wiring") || desc.contains("cable")) return "electrical";
        if (desc.contains("cement") || desc.contains("concrete")) return "cement";
        if (desc.contains("food") || desc.contains("grain") || desc.contains("oil")) return "food";
        if (desc.contains("textile") || desc.contains("fabric") || desc.contains("cloth")) return "textile";
        if (desc.contains("steel") || desc.contains("metal") || desc.contains("iron")) return "mechanical";
        if (desc.contains("chemical") || desc.contains("paint") || desc.contains("coating")) return "chemical";
        if (desc.contains("plastic") || desc.contains("polymer") || desc.contains("pvc")) return "chemical";
        if (desc.contains("electronic") || desc.contains("led") || desc.contains("adapter")) return "electronics";
        if (desc.contains("helmet") || desc.contains("safety")) return "mechanical";
        // Default: use the first significant word
        String[] words = desc.split("\\s+");
        for (String w : words) {
            if (w.length() > 3) return w;
        }
        return desc;
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}