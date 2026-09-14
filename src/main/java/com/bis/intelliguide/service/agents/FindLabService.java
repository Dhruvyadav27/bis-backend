package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.response.LabSearchResponse;
import com.bis.intelliguide.model.Lab;
import com.bis.intelliguide.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FindLabService {

    private final LabRepository labRepository;

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
                        .distanceKm(l.getDistanceMeta())
                        .scope(l.getScope())
                        .build())
                .toList();

        return LabSearchResponse.builder().labs(results).build();
    }
}